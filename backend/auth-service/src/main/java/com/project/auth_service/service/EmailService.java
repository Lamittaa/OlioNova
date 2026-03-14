package com.project.auth_service.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendAccountEmail(String to, String username, String password) {

        try {

            log.info("📧 Preparing account email for {}", to);

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("مرحبًا بك في النظام الذكي لمعصرة الجمعية – بيت جالا");

            String html = """
                    <html dir="rtl" lang="ar">

                    <head>
                        <link href="https://fonts.googleapis.com/css2?family=Tajawal:wght@400;500;700&display=swap" rel="stylesheet">
                    </head>

                    <body style="
                        font-family:'Tajawal', Arial;
                        background-color:#f4f6f8;
                        padding:20px;
                        direction:rtl;
                        text-align:right;
                    ">

                        <div style="
                            max-width:600px;
                            margin:auto;
                            background:white;
                            border-radius:10px;
                            padding:30px;
                            box-shadow:0 4px 12px rgba(0,0,0,0.1);
                        ">

                            <div style="text-align:center;margin-bottom:20px;">
                                <img src="cid:logoImage" width="120"/>
                            </div>

                            <h2 style="
                                color:#2e7d32;
                                text-align:center;
                            ">
                                مرحبًا بك في النظام الذكي لمعصرة الجمعية
                            </h2>

                            <p style="color:#555;font-size:15px;">
                                تم إنشاء حساب لك بنجاح في النظام.
                            </p>

                            <div style="
                                background:#f1f5f9;
                                padding:20px;
                                border-radius:8px;
                                margin-top:20px;
                                font-size:16px;
                            ">

                                <p>
                                <b>اسم المستخدم:</b>
                                <span style="direction:ltr">%s</span>
                                </p>

                                <p>
                                <b>كلمة المرور:</b>
                                <span style="direction:ltr">%s</span>
                                </p>

                            </div>

                            <p style="
                                margin-top:20px;
                                color:#666;
                            ">
                                يمكنك تسجيل الدخول مباشرة إلى النظام.
                                ننصحك بتغيير كلمة المرور بعد أول تسجيل دخول.
                            </p>

                            <div style="text-align:center;margin-top:25px;">

                                <a href="http://localhost:3000/login"
                                   style="
                                   background:#2e7d32;
                                   color:white;
                                   padding:12px 25px;
                                   text-decoration:none;
                                   border-radius:6px;
                                   font-size:15px;
                                   display:inline-block;
                                   ">
                                   تسجيل الدخول للنظام
                                </a>

                            </div>

                            <div style="
                                margin-top:30px;
                                padding-top:10px;
                                border-top:1px solid #eee;
                                font-size:13px;
                                color:#888;
                                text-align:center;
                            ">
                                مع تحيات <br>
                                النظام الذكي لمعصرة الجمعية التعاونية – بيت جالا
                            </div>

                        </div>

                    </body>
                    </html>
                    """
                    .formatted(username, password);

            helper.setText(html, true);

            ClassPathResource logo = new ClassPathResource("static/logo.jpeg");

            helper.addInline("logoImage", logo);

            mailSender.send(message);

            log.info("✅ Email sent successfully to {}", to);

        } catch (Exception e) {

            log.error("❌ Failed to send email", e);

        }
    }
}