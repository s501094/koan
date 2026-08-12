"use strict";

/*
 * Applies a site's Boost, and runs the element picker behind Zap.
 *
 * Everything here is local. The script talks only to the extension's own
 * background page, which talks only to the app over a native port. There is no
 * fetch, no XHR, no beacon, and no third party.
 *
 * Zapped elements are hidden with CSS rather than removed from the DOM. Pages
 * that rebuild their layout would just recreate a removed node, and a stylesheet
 * keeps working across re-renders without us having to watch for them.
 */

const STYLE_ID = "__koan_boost_style";
const PICKER_ID = "__koan_picker_overlay";

let picking = false;
let lastHighlight = null;

function styleElement() {
  let el = document.getElementById(STYLE_ID);
  if (!el) {
    el = document.createElement("style");
    el.id = STYLE_ID;
    // documentElement, not head — at document_start there may be no head yet.
    (document.head || document.documentElement).appendChild(el);
  }
  return el;
}

function apply(rules) {
  const css = [];

  for (const rule of rules) {
    if (rule.zap && rule.zap.length) {
      css.push(`${rule.zap.join(",\n")} { display: none !important; }`);
    }
    if (rule.css) {
      css.push(rule.css);
    }
  }

  styleElement().textContent = css.join("\n");

  for (const rule of rules) {
    if (!rule.js) continue;
    try {
      // Trust boundary: rule.js is script the device owner typed into the
      // Boosts editor. It reaches here only over the native port from the app
      // process — a page cannot reach the background script, so it cannot put
      // anything in `rules`. Running it is the feature, not a hole in it.
      //
      // It runs in the content-script sandbox: full DOM access, isolated from
      // the page's own globals. That limit is deliberate. Injecting into the
      // page world would mean fighting CSP and handing the page a way to
      // notice us.
      // eslint-disable-next-line no-new-func
      new Function(rule.js)();
    } catch (e) {
      console.warn("[koan] boost script failed", e);
    }
  }
}

/* ---- element picker ------------------------------------------------- */

function cssPath(el) {
  if (!(el instanceof Element)) return null;

  // An id is unique and stable enough to stop at.
  if (el.id && /^[A-Za-z][\w-]*$/.test(el.id)) return `#${el.id}`;

  const parts = [];
  let node = el;

  while (node && node.nodeType === Node.ELEMENT_NODE && parts.length < 6) {
    let part = node.nodeName.toLowerCase();

    if (node.id && /^[A-Za-z][\w-]*$/.test(node.id)) {
      parts.unshift(`#${node.id}`);
      break;
    }

    // Prefer stable-looking classes; skip the hashed ones bundlers emit.
    const classes = Array.from(node.classList || [])
      .filter((c) => /^[A-Za-z][\w-]*$/.test(c) && !/\d{4,}|^[a-z]{1,3}-?\d/.test(c))
      .slice(0, 2);

    if (classes.length) {
      part += "." + classes.join(".");
    } else if (node.parentElement) {
      const siblings = Array.from(node.parentElement.children).filter(
        (s) => s.nodeName === node.nodeName
      );
      if (siblings.length > 1) {
        part += `:nth-of-type(${siblings.indexOf(node) + 1})`;
      }
    }

    parts.unshift(part);
    node = node.parentElement;
  }

  return parts.join(" > ");
}

function overlay() {
  let el = document.getElementById(PICKER_ID);
  if (!el) {
    el = document.createElement("div");
    el.id = PICKER_ID;
    el.style.cssText = [
      "position:fixed",
      "pointer-events:none",
      "z-index:2147483647",
      "background:rgba(124,92,255,0.28)",
      "border:2px solid rgba(124,92,255,0.9)",
      "border-radius:4px",
      "transition:all 60ms ease-out",
    ].join(";");
    document.documentElement.appendChild(el);
  }
  return el;
}

function highlight(el) {
  if (!el) return;
  const rect = el.getBoundingClientRect();
  const box = overlay();
  box.style.top = `${rect.top}px`;
  box.style.left = `${rect.left}px`;
  box.style.width = `${rect.width}px`;
  box.style.height = `${rect.height}px`;
  box.style.display = "block";
}

function onMove(event) {
  if (!picking) return;
  const touch = event.touches ? event.touches[0] : event;
  const el = document.elementFromPoint(touch.clientX, touch.clientY);
  if (el && el !== lastHighlight && el.id !== PICKER_ID) {
    lastHighlight = el;
    highlight(el);
  }
}

function onPick(event) {
  if (!picking) return;
  event.preventDefault();
  event.stopPropagation();

  const touch = event.changedTouches ? event.changedTouches[0] : event;
  const el = document.elementFromPoint(touch.clientX, touch.clientY);
  if (!el) return;

  const selector = cssPath(el);
  if (selector) {
    browser.runtime.sendMessage({
      type: "picked",
      selector,
      url: location.href,
    });
  }
  stopPicking();
}

function startPicking() {
  if (picking) return;
  picking = true;
  document.addEventListener("touchmove", onMove, true);
  document.addEventListener("mousemove", onMove, true);
  document.addEventListener("touchend", onPick, true);
  document.addEventListener("click", onPick, true);
}

function stopPicking() {
  picking = false;
  lastHighlight = null;
  document.removeEventListener("touchmove", onMove, true);
  document.removeEventListener("mousemove", onMove, true);
  document.removeEventListener("touchend", onPick, true);
  document.removeEventListener("click", onPick, true);
  const box = document.getElementById(PICKER_ID);
  if (box) box.remove();
}

/* ---- wiring ---------------------------------------------------------- */

browser.runtime.onMessage.addListener((message) => {
  switch (message.type) {
    case "apply":
      apply(message.rules || []);
      break;
    case "startPicker":
      startPicking();
      break;
    case "stopPicker":
      stopPicking();
      break;
  }
});

browser.runtime
  .sendMessage({ type: "requestRules", url: location.href })
  .then((response) => apply((response && response.rules) || []))
  .catch(() => {});
