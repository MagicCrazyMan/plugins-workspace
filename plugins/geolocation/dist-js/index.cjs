'use strict';

var core = require('@tauri-apps/api/core');

// Copyright 2019-2023 Tauri Programme within The Commons Conservancy
// SPDX-License-Identifier: Apache-2.0
// SPDX-License-Identifier: MIT
async function watchPosition(options, cb) {
    const channel = new core.Channel();
    channel.onmessage = (message) => {
        if (typeof message === 'string') {
            cb(null, message);
        }
        else {
            cb(message);
        }
    };
    await core.invoke('plugin:geolocation|watch_position', {
        options,
        channel
    });
    return channel.id;
}
async function getCurrentPosition(options) {
    return await core.invoke('plugin:geolocation|get_current_position', {
        options
    });
}
async function clearWatch(channelId) {
    await core.invoke('plugin:geolocation|clear_watch', {
        channelId
    });
}
async function checkPermissions() {
    return await core.checkPermissions('geolocation');
}
async function requestPermissions(permissions) {
    return await core.invoke('plugin:geolocation|request_permissions', {
        permissions
    });
}

exports.checkPermissions = checkPermissions;
exports.clearWatch = clearWatch;
exports.getCurrentPosition = getCurrentPosition;
exports.requestPermissions = requestPermissions;
exports.watchPosition = watchPosition;
