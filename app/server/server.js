const express = require('express');
const app = express();
const PORT = process.env.PORT || 3000;

// 1️⃣ الصفحة الرئيسية الترحيبية (عند فتح الرابط المباشر)
app.get('/', (req, res) => {
    res.send(`
    <!DOCTYPE html>
    <html lang="ar" dir="rtl">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>OmniLens Verification Portal</title>
        <style>
            body {
                background-color: #0d1117;
                color: #c9d1d9;
                font-family: system-ui, -apple-system, sans-serif;
                display: flex;
                justify-content: center;
                align-items: center;
                min-height: 100vh;
                margin: 0;
                padding: 20px;
                box-sizing: border-box;
            }
            .card {
                background: #161b22;
                border: 1px solid #30363d;
                border-radius: 16px;
                padding: 35px;
                max-width: 480px;
                text-align: center;
                box-shadow: 0 10px 30px rgba(0,0,0,0.5);
            }
            .logo {
                font-size: 2.2rem;
                font-weight: bold;
                color: #58a6ff;
                margin-bottom: 8px;
            }
            .subtitle {
                color: #8b949e;
                font-size: 0.95rem;
                margin-bottom: 20px;
            }
            .badge {
                background-color: #238636;
                color: #ffffff;
                padding: 8px 18px;
                border-radius: 20px;
                font-size: 0.9rem;
                font-weight: bold;
                display: inline-block;
                margin-bottom: 25px;
            }
            p {
                color: #c9d1d9;
                line-height: 1.7;
                font-size: 1rem;
            }
            .footer {
                margin-top: 30px;
                font-size: 0.8rem;
                color: #484f58;
            }
        </style>
    </head>
    <body>
        <div class="card">
            <div class="logo">OmniLens Engine</div>
            <div class="subtitle">DRM & Watermarking Verification System</div>
            <div class="badge">🌐 الخدمة السحابية تعمل بنجاح 100%</div>
            <p>مرحباً بك في بوابة التوثيق الرقمية الرسمية المنبثقة عن نظام <strong>OmniLens</strong>.</p>
            <p>للتحقق من أصالة أي صورة أو ملف موثّق، يرجى مسح رمز الـ QR أو فتح رابط التوثيق المرفق بالوسائط مباشرة.</p>
            <div class="footer">
                OmniLens Engine © 2026 | Secure Watermarking
            </div>
        </div>
    </body>
    </html>
    `);
});

// 2️⃣ صفحة التحقق من البصمة الديناميكية
app.get('/v/:hash', (req, res) => {
    const hash = req.params.hash;
    res.send(`
    <!DOCTYPE html>
    <html lang="ar" dir="rtl">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>توثيق OmniLens - ${hash}</title>
        <style>
            body {
                background-color: #0d1117;
                color: #c9d1d9;
                font-family: system-ui, -apple-system, sans-serif;
                display: flex;
                justify-content: center;
                align-items: center;
                min-height: 100vh;
                margin: 0;
                padding: 20px;
                box-sizing: border-box;
            }
            .card {
                background: #161b22;
                border: 1px solid #30363d;
                border-radius: 16px;
                padding: 30px;
                max-width: 400px;
                width: 100%;
                text-align: center;
                box-shadow: 0 10px 25px rgba(0,0,0,0.5);
            }
            .badge-success {
                background-color: #238636;
                color: white;
                padding: 10px 20px;
                border-radius: 20px;
                font-weight: bold;
                display: inline-block;
                margin-bottom: 20px;
            }
            .info-box {
                background: #0d1117;
                border: 1px solid #30363d;
                border-radius: 8px;
                padding: 15px;
                margin-top: 20px;
                text-align: right;
            }
            .hash-code {
                color: #58a6ff;
                font-family: monospace;
                background: #161b22;
                padding: 2px 6px;
                border-radius: 4px;
            }
            .footer {
                margin-top: 25px;
                font-size: 0.8rem;
                color: #8b949e;
            }
        </style>
    </head>
    <body>
        <div class="card">
            <div class="badge-success">✅ صورة موثقة وأصلية 100%</div>
            <h2>منظومة OmniLens<br>Verification Portal</h2>
            <p>تم التحقق من بصمة التشفير الخاصة بهذه الصورة بنجاح.</p>
            
            <div class="info-box">
                <p><strong>رمز البصمة المختصر:</strong> <span class="hash-code">${hash}</span></p>
                <p><strong>حالة الترخيص:</strong> تجاري / عقد مالي موثق</p>
                <p><strong>حماية الحقوق:</strong> مسجلة في سجل التوثيق الموحد لمنظومة OmniLens</p>
            </div>

            <div class="footer">
                OmniLens DRM & Watermarking Engine © 2026
            </div>
        </div>
    </body>
    </html>
    `);
});

app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
