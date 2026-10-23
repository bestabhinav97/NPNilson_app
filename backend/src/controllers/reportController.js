const multer = require('multer');
const path = require('path');
const { v4: uuidv4, validate: isUuid } = require('uuid');
const reportService = require('../services/reportService');
const authService = require('../services/authService');

const MAX_IMAGES = 5;
const MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

const IMAGE_EXTENSIONS = {
    'image/jpeg': '.jpg',
    'image/png': '.png',
    'image/webp': '.webp',
    'image/heic': '.heic'
};

const upload = multer({
    storage: multer.diskStorage({
        destination: reportService.UPLOAD_DIR,

        // File name is "<imageId>.<ext>" so the image id
        // can be derived from the saved file name.
        filename: (req, file, cb) => {
            cb(
                null,
                uuidv4() + IMAGE_EXTENSIONS[file.mimetype]
            );
        }
    }),

    limits: {
        fileSize: MAX_IMAGE_SIZE_BYTES,
        files: MAX_IMAGES
    },

    fileFilter: (req, file, cb) => {
        if (!IMAGE_EXTENSIONS[file.mimetype]) {
            return cb(
                new Error(
                    'Only JPEG, PNG, WEBP or HEIC images are allowed'
                )
            );
        }

        cb(null, true);
    }
}).array(
    'images',
    MAX_IMAGES
);

// ---------------------------------------------------------
// Upload middleware
// ---------------------------------------------------------

function uploadImages(req, res, next) {
    upload(
        req,
        res,
        err => {
            if (!err) {
                return next();
            }

            if (
                err instanceof multer.MulterError
            ) {
                const messages = {
                    LIMIT_FILE_SIZE:
                        'Each image must be at most 10 MB',

                    LIMIT_FILE_COUNT:
                        `A report can have at most ${MAX_IMAGES} images`,

                    LIMIT_UNEXPECTED_FILE:
                        `A report can have at most ${MAX_IMAGES} images`
                };

                return res
                    .status(400)
                    .json({
                        error:
                            messages[err.code] ||
                            err.message
                    });
            }

            return res
                .status(400)
                .json({
                    error: err.message
                });
        }
    );
}

// ---------------------------------------------------------
// Reports
// ---------------------------------------------------------

async function createReport(req, res) {
    try {
        const report =
            await reportService.createReport(
                req.user,
                req.body,
                req.files
            );

        return res
            .status(201)
            .json(report);
    } catch (err) {
        return res
            .status(400)
            .json({
                error: err.message
            });
    }
}

async function getReports(req, res) {
    try {
        const reports =
            await reportService.getReportsForUser(
                req.user
            );

        return res.json(
            reports
        );
    } catch (err) {
        return res
            .status(500)
            .json({
                error: err.message
            });
    }
}

// ---------------------------------------------------------
// Admin report status update
// ---------------------------------------------------------

async function updateReportStatus(req, res) {
    try {
        const { reportId } =
            req.params;

        const { status } =
            req.body;

        if (!isUuid(reportId)) {
            return res
                .status(404)
                .json({
                    error: 'Report not found'
                });
        }

        const report =
            await reportService.updateReportStatus(
                reportId,
                status
            );

        if (!report) {
            return res
                .status(404)
                .json({
                    error: 'Report not found'
                });
        }

        // Record the admin action in the activity feed.
        await authService.recordAdminActivity(
            req.user?.id,
            'REPORT_STATUS_UPDATED',
            'REPORT',
            reportId,
            `${report.title || 'Rapport'} → ${statusLabel(status)}`
        );

        return res.json(
            report
        );
    } catch (err) {
        return res
            .status(400)
            .json({
                error: err.message
            });
    }
}

// ---------------------------------------------------------
// Report images
// ---------------------------------------------------------

async function getReportImage(req, res) {
    try {
        const {
            reportId,
            imageId
        } = req.params;

        if (
            !isUuid(reportId) ||
            !isUuid(imageId)
        ) {
            return res
                .status(404)
                .json({
                    error: 'Image not found'
                });
        }

        const image =
            await reportService.getReportImage(
                req.user,
                reportId,
                imageId
            );

        if (!image) {
            return res
                .status(404)
                .json({
                    error: 'Image not found'
                });
        }

        res.type(
            image.contentType
        );

        return res.sendFile(
            path.resolve(
                image.path
            )
        );
    } catch (err) {
        return res
            .status(500)
            .json({
                error: err.message
            });
    }
}

// ---------------------------------------------------------
// Helpers
// ---------------------------------------------------------

function statusLabel(status) {
    switch (status) {
        case 'OPEN':
            return 'Ny';

        case 'IN_PROGRESS':
            return 'Pågår';

        case 'RESOLVED':
            return 'Klar';

        default:
            return status || 'Okänd';
    }
}

// ---------------------------------------------------------
// Exports
// ---------------------------------------------------------

module.exports = {
    uploadImages,
    createReport,
    getReports,
    updateReportStatus,
    getReportImage
};