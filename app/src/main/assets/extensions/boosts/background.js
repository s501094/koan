"use strict";

/*
 * Relay between the app and the content scripts.
 *
 * Rules are pushed here from the app and cached, so a content script starting
 * at document_start gets an answer immediately instead of waiting on a native
 * round trip and flashing unstyled content first.
 */

let rules = [];
let pickerTabs = new Set();

const port = browser.runtime.connectNative("koanBoosts");

port.onMessage.addListener((message) => {
  switch (message.type) {
    case "rules":
      rules = Array.isArray(message.rules) ? message.rules : [];
      broadcast();
      break;
    case "startPicker":
      pickerTabs.add(message.tabId ?? -1);
      sendToActive({ type: "startPicker" });
      break;
    case "stopPicker":
      pickerTabs.clear();
      sendToActive({ type: "stopPicker" });
      break;
  }
});

function matches(rule, url) {
  if (!rule.enabled) return false;
  try {
    const host = new URL(url).hostname;
    // A leading dot means "this domain and anything under it".
    if (rule.pattern.startsWith(".")) {
      return host === rule.pattern.slice(1) || host.endsWith(rule.pattern);
    }
    return host === rule.pattern;
  } catch (e) {
    return false;
  }
}

function rulesFor(url) {
  return rules.filter((r) => matches(r, url));
}

function broadcast() {
  browser.tabs.query({}).then((tabs) => {
    for (const tab of tabs) {
      browser.tabs
        .sendMessage(tab.id, { type: "apply", rules: rulesFor(tab.url || "") })
        .catch(() => {});
    }
  });
}

function sendToActive(payload) {
  browser.tabs.query({ active: true }).then((tabs) => {
    for (const tab of tabs) {
      browser.tabs.sendMessage(tab.id, payload).catch(() => {});
    }
  });
}

browser.runtime.onMessage.addListener((message, sender) => {
  if (message.type === "requestRules") {
    return Promise.resolve({ rules: rulesFor(message.url || "") });
  }
  if (message.type === "picked") {
    port.postMessage({
      type: "picked",
      selector: message.selector,
      url: sender.tab ? sender.tab.url : message.url,
    });
    return Promise.resolve({ ok: true });
  }
  return false;
});
