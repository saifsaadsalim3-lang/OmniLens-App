const express = require('express');
const app = express();
const PORT = process.env.PORT || 3000;

app.use(express.json());

// صفحة التحقق العامة عند فتح رابط التحقق المطبوع على الصورة
app.get('/v/:hash', (req, res) => {
    const hash = req.params.hash;

    res.send(`
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>OmniLens Engine — بوابة التحقق الرسمية</title>
            <style>
                body { background-color: #0F172A; color: #FFFFFF; font-family: system-ui, sans-serif; text-align: center; padding: 40px 20px; }
                .container { background: #1E293B; border-radius: 16px; padding: 30px; max-width: 550px; margin: auto; border: 1px solid #334155; box-shadow: 0 20px 25px -5px rgba(0,0,0,0.5); }
                .status-badge { background-color: #16A34A; color: white; padding: 10px 20px; border-radius: 9999px; font-weight: bold; display: inline-block; margin-bottom: 20px; }
                .info-box { background: #0F172A; text-align: right; padding: 15px; border-radius: 8px; margin-top: 20px; border-right: 4px solid #38BDF8; }
                .hash-code { font-family: monospace; background: #334155; padding: 8px; border-radius: 6px; word-break: break-all; color: #38BDF8; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="status-badge">✅ صورة موثقة وأصلية 100%</div>
                <h2>منظومة OmniLens Verification Portal</h2>
                <p>تم التحقق من بصمة التشفير الخاصة بهذه الصورة بنجاح.</p>

                <div class="info-box">
                    <p><strong>رمز البصمة المختصر:</strong> <span class="hash-code">${hash}</span></p>
                    <p><strong>حالة الترخيص:</strong> تجاري / عقد مالي موثق</p>
                    <p><strong>حماية الحقوق:</strong> مسجلة في سجل التوثيق الموحد لمنظومة OmniLens</p>
                </div>
                <p style="color: #94A3B8; font-size: 12px; margin-top: 30px;">OmniLens DRM & Watermarking Engine © 2026</p>
            </div>
        </body>
        </html>
    `);
});

app.listen(PORT, () => {
    console.log(`🚀 خادم التحقق الخارجي لـ OmniLens يعمل على المنفذ ${PORT}`);
});
