const exifr = require('exifr');

// Photos whose EXIF has no time zone offset are assumed to be taken in this zone (all stores are in Sweden)
const PHOTO_TIME_ZONE = process.env.PHOTO_TIME_ZONE || 'Europe/Stockholm';
const MAX_TEXT_LENGTH = 100;

const EMPTY_METADATA = {
    takenAtLocal: null,
    takenAtOffset: null,
    latitude: null,
    longitude: null,
    altitude: null,
    cameraMake: null,
    cameraModel: null,
    exif: null
};

// EXIF "2026:10:18 14:05:09" -> "2026-10-18T14:05:09", or null if it isn't a real date (e.g. "0000:00:00 00:00:00")
function toIsoLocal(exifDateTime) {
    const match = /^(\d{4}):(\d{2}):(\d{2}) (\d{2}):(\d{2}):(\d{2})$/.exec(String(exifDateTime || '').trim());
    if (!match) return null;
    const iso = `${match[1]}-${match[2]}-${match[3]}T${match[4]}:${match[5]}:${match[6]}`;
    const date = new Date(`${iso}Z`);
    if (Number.isNaN(date.getTime()) || date.getUTCFullYear() < 1990) return null;
    return iso;
}

function toOffset(value) {
    const offset = String(value || '').trim();
    return /^[+-]\d{2}:\d{2}$/.test(offset) ? offset : null;
}

function toNumber(value) {
    return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function toText(value) {
    if (typeof value !== 'string') return null;
    const text = value.replace(/\0/g, '').trim();
    return text ? text.slice(0, MAX_TEXT_LENGTH) : null;
}

function altitudeOf(exif) {
    const altitude = toNumber(exif.GPSAltitude);
    if (altitude === null) return null;
    // GPSAltitudeRef 1 means below sea level; exifr returns it as a number or a byte array
    const ref = exif.GPSAltitudeRef && typeof exif.GPSAltitudeRef === 'object' ? exif.GPSAltitudeRef[0] : exif.GPSAltitudeRef;
    return Number(ref) === 1 ? -altitude : altitude;
}

// Keeps only plain values so the raw EXIF fits in a JSONB column (drops binary maker notes etc.)
function plainValues(exif) {
    const result = {};
    for (const [key, value] of Object.entries(exif)) {
        if (typeof value === 'string') {
            const text = value.replace(/\0/g, '');
            if (text.length <= 500) result[key] = text;
        } else if ((typeof value === 'number' && Number.isFinite(value)) || typeof value === 'boolean') {
            result[key] = value;
        } else if (Array.isArray(value) && value.length <= 10 && value.every(v => typeof v === 'number' && Number.isFinite(v))) {
            result[key] = value;
        }
    }
    return result;
}

// Reads the camera metadata of an uploaded image. Never throws: a photo without readable EXIF gives empty metadata.
async function readPhotoMetadata(filePath) {
    let exif;
    try {
        exif = await exifr.parse(filePath, { gps: true, reviveValues: false });
    } catch (err) {
        return { ...EMPTY_METADATA };
    }
    if (!exif) return { ...EMPTY_METADATA };

    const latitude = toNumber(exif.latitude);
    const longitude = toNumber(exif.longitude);
    const hasPosition = latitude !== null && longitude !== null && !(latitude === 0 && longitude === 0);

    return {
        takenAtLocal: toIsoLocal(exif.DateTimeOriginal || exif.CreateDate || exif.ModifyDate),
        takenAtOffset: toOffset(exif.OffsetTimeOriginal || exif.OffsetTime),
        latitude: hasPosition ? latitude : null,
        longitude: hasPosition ? longitude : null,
        altitude: hasPosition ? altitudeOf(exif) : null,
        cameraMake: toText(exif.Make),
        cameraModel: toText(exif.Model),
        exif: plainValues(exif)
    };
}

module.exports = {
    PHOTO_TIME_ZONE,
    readPhotoMetadata
};
