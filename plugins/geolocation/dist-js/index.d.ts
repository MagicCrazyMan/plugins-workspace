import { PermissionState } from '@tauri-apps/api/core';
export type Coordinates = {
    /**
     * Latitude in decimal degrees.
     */
    latitude: number;
    /**
     * Longitude in decimal degrees.
     */
    longitude: number;
    /**
     * Accuracy level of the latitude and longitude coordinates in meters.
     */
    accuracy: number;
    /**
     * Accuracy level of the altitude coordinate in meters, if available.
     * Available on all iOS versions and on Android 8 and above.
     */
    altitudeAccuracy: number | null;
    /**
     * The altitude the user is at, if available.
     */
    altitude: number | null;
    speed: number | null;
    /**
     * The heading the user is facing, if available.
     */
    heading: number | null;
};
export type PermissionStatus = {
    /**
     * Permission state for the location alias.
     *
     * On Android it requests/checks both ACCESS_COARSE_LOCATION and ACCESS_FINE_LOCATION permissions.
     *
     * On iOS it requests/checks location permissions.
     */
    location: PermissionState;
    /**
     * Permissions state for the coarseLoaction alias.
     *
     * On Android it requests/checks ACCESS_COARSE_LOCATION.
     *
     * On Android 12+, users can choose between Approximate location (ACCESS_COARSE_LOCATION) and Precise location (ACCESS_FINE_LOCATION).
     *
     * On iOS it will have the same value as the `location` alias.
     */
    coarseLocation: PermissionState;
};
export type PermissionType = 'location' | 'coarseLocation';
export type Position = {
    /**
     * Creation time for these coordinates.
     */
    timestamp: number;
    /**
     * The GPD coordinates along with the accuracy of the data.
     */
    coords: Coordinates;
};
export type PositionOptions = {
    /**
     * High accuracy mode (such as GPS, if available)
     * Will be ignored on Android 12+ if users didn't grant the ACCESS_FINE_LOCATION permission (`coarseLocation` permission).
     */
    enableHighAccuracy: boolean;
    /**
     * The maximum wait time in milliseconds for location updates or getting a position.
    *
     * Ignored on iOS
     */
    timeout: number;
    /**
     * The maximum age in milliseconds of a possible cached position that is acceptable to return.
     * Default: 0
     * Ignored on iOS
     */
    maximumAge: number;
    /**
     * Uses Google Play Location API or Android Framework Location API to get current position.
     * Default: true
     * Only Available for getCurrentPosition on Android.
     */
    useGMS?: boolean;
};
export declare function watchPosition(options: PositionOptions, cb: (location: Position | null, error?: string) => void): Promise<number>;
export declare function getCurrentPosition(options?: PositionOptions): Promise<Position>;
export declare function clearWatch(channelId: number): Promise<void>;
export declare function checkPermissions(): Promise<PermissionStatus>;
export declare function requestPermissions(permissions: PermissionType[] | null): Promise<PermissionStatus>;
