package com.carslab.crm.modules.email.domain.services

import com.carslab.crm.modules.email.domain.model.ProtocolEmailData
import com.carslab.crm.modules.company_settings.domain.model.CompanySettings
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class EmailTemplateService {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        // Profesjonalny szablon HTML dla detailingu - Dark Premium Style
        private const val DEFAULT_PROTOCOL_TEMPLATE = """
<!DOCTYPE html>
<html lang="pl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Protokół przyjęcia pojazdu</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: 'Inter', 'Segoe UI', system-ui, -apple-system, sans-serif;
            line-height: 1.6;
            background: #0f0f0f;
            margin: 0;
            padding: 20px;
            color: #ffffff;
        }
        
        .email-container {
            max-width: 720px;
            margin: 0 auto;
            background: linear-gradient(145deg, #1a1a1a 0%, #2d2d2d 100%);
            border-radius: 20px;
            box-shadow: 
                0 25px 50px rgba(0, 0, 0, 0.5),
                0 0 0 1px rgba(255, 255, 255, 0.05);
            overflow: hidden;
            border: 1px solid #333333;
        }
        
        .header {
            background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 50%, #ff9ff3 100%);
            background-size: 400% 400%;
            animation: gradientShift 8s ease infinite;
            color: white;
            text-align: center;
            padding: 50px 40px;
            position: relative;
            overflow: hidden;
        }
        
        @keyframes gradientShift {
            0% { background-position: 0% 50%; }
            50% { background-position: 100% 50%; }
            100% { background-position: 0% 50%; }
        }
        
        .header::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: 
                radial-gradient(circle at 20% 80%, rgba(255, 255, 255, 0.1) 0%, transparent 50%),
                radial-gradient(circle at 80% 20%, rgba(255, 255, 255, 0.1) 0%, transparent 50%);
        }
        
        .company-logo {
            font-size: 36px;
            font-weight: 700;
            letter-spacing: 3px;
            margin-bottom: 15px;
            position: relative;
            z-index: 1;
            text-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
        }
        
        .header-subtitle {
            font-size: 16px;
            font-weight: 400;
            opacity: 0.95;
            position: relative;
            z-index: 1;
            letter-spacing: 1px;
            text-transform: uppercase;
        }
        
        .content {
            padding: 60px 50px;
            background: #1a1a1a;
        }
        
        .greeting {
            font-size: 28px;
            font-weight: 600;
            color: #ffffff;
            margin-bottom: 25px;
            text-align: center;
        }
        
        .introduction {
            font-size: 18px;
            color: #b8b8b8;
            text-align: center;
            margin-bottom: 50px;
            line-height: 1.8;
            max-width: 500px;
            margin-left: auto;
            margin-right: auto;
        }
        
        .protocol-card {
            background: linear-gradient(145deg, #2a2a2a 0%, #1f1f1f 100%);
            border-radius: 16px;
            padding: 40px;
            margin: 40px 0;
            border: 1px solid #404040;
            position: relative;
            box-shadow: 
                inset 0 1px 0 rgba(255, 255, 255, 0.1),
                0 10px 30px rgba(0, 0, 0, 0.3);
        }
        
        .protocol-card::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            height: 3px;
            background: linear-gradient(90deg, #ff6b6b 0%, #ee5a24 50%, #ff9ff3 100%);
            border-radius: 16px 16px 0 0;
        }
        
        .protocol-title {
            font-size: 24px;
            font-weight: 600;
            color: #ffffff;
            margin-bottom: 35px;
            text-align: center;
            position: relative;
        }
        
        .protocol-title::after {
            content: '';
            position: absolute;
            bottom: -15px;
            left: 50%;
            transform: translateX(-50%);
            width: 60px;
            height: 2px;
            background: linear-gradient(90deg, #ff6b6b, #ee5a24);
        }
        
        .vehicle-info {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 25px;
            margin-bottom: 30px;
        }
        
        @media (max-width: 600px) {
            .vehicle-info {
                grid-template-columns: 1fr;
                gap: 20px;
            }
            .content {
                padding: 40px 30px;
            }
        }
        
        .info-item {
            background: linear-gradient(145deg, #333333 0%, #2a2a2a 100%);
            padding: 25px;
            border-radius: 12px;
            border: 1px solid #404040;
            position: relative;
            transition: all 0.3s ease;
            box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
        }
        
        .info-item::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            width: 4px;
            height: 100%;
            background: linear-gradient(180deg, #ff6b6b 0%, #ee5a24 100%);
            border-radius: 12px 0 0 12px;
        }
        
        .info-label {
            font-size: 11px;
            font-weight: 700;
            color: #888888;
            text-transform: uppercase;
            letter-spacing: 1.5px;
            margin-bottom: 12px;
        }
        
        .info-value {
            font-size: 20px;
            font-weight: 600;
            color: #ffffff;
            letter-spacing: 0.5px;
        }
        
        .service-period {
            background: linear-gradient(145deg, #333333 0%, #2a2a2a 100%);
            padding: 30px;
            border-radius: 12px;
            border: 1px solid #404040;
            text-align: center;
            margin-top: 30px;
            box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
            position: relative;
        }
        
        .service-period::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            height: 3px;
            background: linear-gradient(90deg, #ff6b6b 0%, #ee5a24 50%, #ff9ff3 100%);
            border-radius: 12px 12px 0 0;
        }
        
        .period-label {
            font-size: 12px;
            font-weight: 700;
            color: #888888;
            text-transform: uppercase;
            letter-spacing: 1.5px;
            margin-bottom: 15px;
        }
        
        .period-value {
            font-size: 22px;
            font-weight: 600;
            color: #ffffff;
            letter-spacing: 0.5px;
        }
        
        .contact-section {
            background: linear-gradient(145deg, #2d2d2d 0%, #1a1a1a 100%);
            border-top: 1px solid #404040;
            padding: 50px 50px;
            text-align: center;
        }
        
        .contact-title {
            font-size: 28px;
            font-weight: 600;
            margin-bottom: 30px;
            color: #ffffff;
        }
        
        .contact-info {
            display: flex;
            justify-content: center;
            gap: 50px;
            margin-top: 30px;
            flex-wrap: wrap;
        }
        
        .contact-item {
            text-align: center;
            background: linear-gradient(145deg, #333333 0%, #2a2a2a 100%);
            padding: 25px 30px;
            border-radius: 12px;
            border: 1px solid #404040;
            min-width: 150px;
            box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
        }
        
        .contact-label {
            font-size: 11px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 1.5px;
            margin-bottom: 10px;
            color: #888888;
        }
        
        .contact-value {
            font-size: 16px;
            font-weight: 500;
            color: #ffffff;
        }
        
        .address-item {
            background: linear-gradient(145deg, #333333 0%, #2a2a2a 100%);
            padding: 25px;
            border-radius: 12px;
            border: 1px solid #404040;
            margin-top: 25px;
            box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
        }
        
        .footer {
            background: linear-gradient(145deg, #1a1a1a 0%, #0f0f0f 100%);
            padding: 40px 50px;
            text-align: center;
            border-top: 1px solid #404040;
        }
        
        .footer-message {
            font-size: 18px;
            margin-bottom: 20px;
            color: #ffffff;
            font-weight: 500;
        }
        
        .footer-note {
            font-size: 14px;
            color: #888888;
            font-style: italic;
            line-height: 1.6;
        }
        
        .highlight {
            background: linear-gradient(120deg, #ff6b6b 0%, #ee5a24 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
            font-weight: 700;
        }
    </style>
</head>
<body>
    <div class="email-container">
        <div class="header">
            <div class="company-logo">{{COMPANY_NAME}}</div>
            <div class="header-subtitle">Premium Automotive Detailing</div>
        </div>

        <div class="content">
            <div class="greeting">
                Szanowni Państwo {{CLIENT_NAME}}
            </div>

            <div class="introduction">
                Dziękujemy za wybór naszych usług detailingowych. 
                <br>Przesyłamy protokół przyjęcia Państwa pojazdu wraz z harmonogramem realizacji usług.
            </div>

            <div class="protocol-card">
                <div class="protocol-title">Szczegóły zlecenia</div>
                
                <div class="vehicle-info">
                    <div class="info-item">
                        <div class="info-label">Pojazd</div>
                        <div class="info-value">{{VEHICLE_MAKE}} {{VEHICLE_MODEL}}</div>
                    </div>
                    
                    <div class="info-item">
                        <div class="info-label">Nr rejestracyjny</div>
                        <div class="info-value">{{LICENSE_PLATE}}</div>
                    </div>
                </div>

                <div class="service-period">
                    <div class="period-label">Okres realizacji usług</div>
                    <div class="period-value">{{SERVICE_PERIOD}}</div>
                </div>
            </div>
        </div>

        <div class="contact-section">
            <div class="contact-title">Kontakt w sprawie zlecenia</div>
            
            <div class="contact-info">
                {{#if COMPANY_PHONE}}
                <div class="contact-item">
                    <div class="contact-label">Telefon</div>
                    <div class="contact-value">{{COMPANY_PHONE}}</div>
                </div>
                {{/if}}
                
                {{#if COMPANY_EMAIL}}
                <div class="contact-item">
                    <div class="contact-label">Email</div>
                    <div class="contact-value">{{COMPANY_EMAIL}}</div>
                </div>
                {{/if}}
            </div>
            
            {{#if COMPANY_ADDRESS}}
            <div class="address-item">
                <div class="contact-label">Adres</div>
                <div style="font-size: 16px; color: #ffffff;">{{COMPANY_ADDRESS}}</div>
            </div>
            {{/if}}
        </div>

        <div class="footer">
            <div class="footer-message">
                Dziękujemy za <span class="highlight">zaufanie</span> i wybór naszego studia detailingowego
            </div>
            <div class="footer-note">
                Ta wiadomość została wygenerowana automatycznie. W razie pytań prosimy o kontakt telefoniczny.
            </div>
        </div>
    </div>
</body>
</html>
        """
    }

    fun generateProtocolEmail(
        protocolData: ProtocolEmailData,
        companySettings: CompanySettings,
        additionalVariables: Map<String, String> = emptyMap()
    ): String {
        var content = DEFAULT_PROTOCOL_TEMPLATE

        // Formatowanie okresu serwisu
        val formattedServicePeriod = formatServicePeriod(protocolData.servicePeriod)

        // Podstawowe zmienne
        val variables = mutableMapOf(
            "COMPANY_NAME" to companySettings.basicInfo.companyName,
            "CLIENT_NAME" to protocolData.clientName,
            "VEHICLE_MAKE" to protocolData.vehicleMake,
            "VEHICLE_MODEL" to protocolData.vehicleModel,
            "LICENSE_PLATE" to protocolData.licensePlate,
            "SERVICE_PERIOD" to formattedServicePeriod,
            "COMPANY_ADDRESS" to (companySettings.basicInfo.address ?: ""),
            "COMPANY_PHONE" to (companySettings.basicInfo.phone ?: ""),
            "COMPANY_EMAIL" to (companySettings.emailSettings?.senderEmail ?: "")
        )

        // Dodaj dodatkowe zmienne
        variables.putAll(additionalVariables)

        // Zastąp wszystkie zmienne
        variables.forEach { (key, value) ->
            content = content.replace("{{$key}}", value)
        }

        // Obsługa warunków {{#if}}
        content = handleConditionalBlocks(content, variables)

        return content
    }

    fun generateSubject(protocolData: ProtocolEmailData): String {
        return "Protokół przyjęcia pojazdu ${protocolData.vehicleMake} ${protocolData.vehicleModel} • ${protocolData.licensePlate}"
    }

    private fun formatServicePeriod(servicePeriod: String): String {
        try {
            // Oczekujemy formatu: "2025-06-17T16:31:00 - 2025-06-17T23:59:59"
            val parts = servicePeriod.split(" - ")

            if (parts.size == 2) {
                val startDateTime = LocalDateTime.parse(parts[0])
                val endDateTime = LocalDateTime.parse(parts[1])

                val startFormatted = startDateTime.format(DATETIME_FORMATTER)
                val endFormatted = endDateTime.format(DATE_FORMATTER)

                return "$startFormatted - $endFormatted"
            }
        } catch (e: Exception) {
            // Jeśli parsowanie się nie uda, zwróć oryginalną wartość
        }

        return servicePeriod
    }

    private fun handleConditionalBlocks(content: String, variables: Map<String, String>): String {
        var result = content

        // Znajdź wszystkie bloki {{#if VARIABLE}}...{{/if}}
        val conditionalRegex = Regex("\\{\\{#if (\\w+)\\}\\}(.*?)\\{\\{/if\\}\\}", RegexOption.DOT_MATCHES_ALL)

        result = conditionalRegex.replace(result) { matchResult ->
            val variableName = matchResult.groupValues[1]
            val blockContent = matchResult.groupValues[2]
            val variableValue = variables[variableName]

            // Pokaż blok tylko jeśli zmienna istnieje i nie jest pusta
            if (!variableValue.isNullOrBlank()) {
                blockContent
            } else {
                ""
            }
        }

        return result
    }
}