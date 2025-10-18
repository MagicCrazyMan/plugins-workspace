import { Channel, invoke, checkPermissions as checkPermissions$1 } from '@tauri-apps/api/core';

// Copyright 2019-2023 Tauri Programme within The Commons Conservancy
// SPDX-License-Identifier: Apache-2.0
// SPDX-License-Identifier: MIT
async function watchPosition(options, cb) {
    const channel = new Channel();
    channel.onmessage = (message) => {
        if (typeof message === 'string') {
            cb(null, message);
        }
        else {
            cb(message);
        }
    };
    await invoke('plugin:geolocation|watch_position', {
        options,
        channel
    });
    return channel.id;
}
async function getCurrentPosition(options) {
    return await invoke('plugin:geolocation|get_current_position', {
        options
    });
}
async function clearWatch(channelId) {
    await invoke('plugin:geolocation|clear_watch', {
        channelId
    });
}
async function checkPermissions() {
    return await checkPermissions$1('geolocation');
}
async function requestPermissions(permissions) {
    return await invoke('plugin:geolocation|request_permissions', {
        permissions
    });
}

export { checkPermissions, clearWatch, getCurrentPosition, requestPermissions, watchPosition };
