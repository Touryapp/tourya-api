package com.tourya.api.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para generar códigos QR.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Slf4j
@Service
public class QrCodeService {

    private static final int QR_WIDTH = 300;
    private static final int QR_HEIGHT = 300;
    private static final String IMAGE_FORMAT = "PNG";

    /**
     * Genera un código QR como array de bytes con colores personalizados y logo.
     * 
     * @param qrContent Contenido del código QR
     * @return Array de bytes de la imagen PNG
     * @throws WriterException si hay error al generar el QR
     * @throws IOException si hay error al convertir a bytes
     */
    public byte[] generateQrCodeImage(String qrContent) throws WriterException, IOException {
        log.debug("Generating custom QR code for content: {}", qrContent);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        
        // Configurar hints para el QR con mayor corrección de errores para el logo
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // Alto para permitir logo
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        // Generar la matriz del código QR
        BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT, hints);

        // Convertir a imagen con colores personalizados
        BufferedImage qrImage = createCustomQrImage(bitMatrix);

        // Convertir a bytes PNG
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(qrImage, IMAGE_FORMAT, outputStream);
        
        byte[] qrImageBytes = outputStream.toByteArray();
        outputStream.close();

        log.debug("Generated custom QR code with {} bytes", qrImageBytes.length);
        return qrImageBytes;
    }

    /**
     * Genera un código QR como array de bytes con tamaño personalizado.
     * 
     * @param qrContent Contenido del código QR
     * @param width Ancho de la imagen
     * @param height Alto de la imagen
     * @return Array de bytes de la imagen PNG
     * @throws WriterException si hay error al generar el QR
     * @throws IOException si hay error al convertir a bytes
     */
    public byte[] generateQrCodeImage(String qrContent, int width, int height) throws WriterException, IOException {
        log.debug("Generating QR code for content: {} with size {}x{}", qrContent, width, height);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        
        // Configurar hints para el QR
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        // Generar la matriz del código QR
        BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, width, height, hints);

        // Convertir a imagen PNG
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, IMAGE_FORMAT, outputStream);
        
        byte[] qrImageBytes = outputStream.toByteArray();
        outputStream.close();

        log.debug("Generated QR code with {} bytes", qrImageBytes.length);
        return qrImageBytes;
    }

    /**
     * Crea una imagen QR personalizada con colores específicos y logo en el centro.
     * 
     * @param bitMatrix Matriz de bits del QR
     * @return BufferedImage con colores personalizados y logo
     * @throws IOException si hay error al cargar el logo
     */
    private BufferedImage createCustomQrImage(BitMatrix bitMatrix) throws IOException {
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        
        // Crear imagen base
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        
        // Configurar renderizado de alta calidad
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        
        // Color de fondo (blanco)
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        
        // Color personalizado para el QR (#1b6475)
        Color qrColor = Color.decode("#1b6475");
        graphics.setColor(qrColor);
        
        // Dibujar el patrón del QR
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (bitMatrix.get(x, y)) {
                    graphics.fillRect(x, y, 1, 1);
                }
            }
        }
        
        // Agregar logo en el centro (área de 60x60 píxeles)
        addLogoToCenter(graphics, width, height);
        
        graphics.dispose();
        return image;
    }
    
    /**
     * Agrega el logo de Tourya en el centro del QR.
     * 
     * @param graphics Objeto Graphics2D para dibujar
     * @param qrWidth Ancho del QR
     * @param qrHeight Alto del QR
     */
    private void addLogoToCenter(Graphics2D graphics, int qrWidth, int qrHeight) {
        try {
            // Tamaño del logo (60x60 píxeles)
            int logoSize = 60;
            int logoX = (qrWidth - logoSize) / 2;
            int logoY = (qrHeight - logoSize) / 2;
            
            // Crear un fondo blanco para el logo
            graphics.setColor(Color.WHITE);
            graphics.fillRoundRect(logoX - 5, logoY - 5, logoSize + 10, logoSize + 10, 10, 10);
            
            // Intentar cargar el logo desde resources
            BufferedImage logo = loadLogo();
            if (logo != null) {
                // Dibujar el logo redimensionado
                graphics.drawImage(logo, logoX, logoY, logoSize, logoSize, null);
            } else {
                // Si no se encuentra el logo, dibujar un círculo con las iniciales "T"
                drawFallbackLogo(graphics, logoX, logoY, logoSize);
            }
            
        } catch (Exception e) {
            log.warn("Could not add logo to QR code, using fallback: {}", e.getMessage());
            // Dibujar logo de respaldo
            int logoSize = 60;
            int logoX = (qrWidth - logoSize) / 2;
            int logoY = (qrHeight - logoSize) / 2;
            drawFallbackLogo(graphics, logoX, logoY, logoSize);
        }
    }
    
    /**
     * Carga el logo desde los recursos de la aplicación.
     * 
     * @return BufferedImage del logo o null si no se encuentra
     */
    private BufferedImage loadLogo() {
        try {
            // Buscar logo en diferentes ubicaciones (orden de prioridad)
            String[] logoPaths = {
                "static/images/logo.png",
                "static/images/logo.jpeg",
                "static/images/logo.jpg",
                "static/images/tourya-logo.png",
                "static/images/tourya-logo.jpeg",
                "images/logo.png",
                "images/logo.jpeg",
                "logo.png",
                "logo.jpeg",
                "static/logo.png",
                "static/logo.jpeg",
                "resources/logo.png"
            };
            
            for (String logoPath : logoPaths) {
                try {
                    log.debug("Searching for logo at: {}", logoPath);
                    ClassPathResource resource = new ClassPathResource(logoPath);
                    if (resource.exists()) {
                        log.info("Found logo at: {}", logoPath);
                        BufferedImage logo = ImageIO.read(resource.getInputStream());
                        if (logo != null) {
                            log.info("Successfully loaded logo from: {}", logoPath);
                            return logo;
                        }
                    }
                } catch (Exception e) {
                    log.debug("Could not load logo from {}: {}", logoPath, e.getMessage());
                }
            }
            
            log.warn("No logo found in any of the configured paths. Using fallback logo.");
            return null;
        } catch (Exception e) {
            log.error("Error searching for logo: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Dibuja un logo de respaldo cuando no se encuentra el archivo de logo.
     * 
     * @param graphics Objeto Graphics2D para dibujar
     * @param x Posición X
     * @param y Posición Y  
     * @param size Tamaño del logo
     */
    private void drawFallbackLogo(Graphics2D graphics, int x, int y, int size) {
        // Color del logo de respaldo (mismo color del QR)
        Color logoColor = Color.decode("#1b6475");
        graphics.setColor(logoColor);
        
        // Dibujar círculo de fondo
        graphics.fillOval(x + 5, y + 5, size - 10, size - 10);
        
        // Dibujar la letra "T" en blanco
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Arial", Font.BOLD, size - 20));
        FontMetrics metrics = graphics.getFontMetrics();
        String text = "T";
        int textX = x + (size - metrics.stringWidth(text)) / 2;
        int textY = y + (size - metrics.getHeight()) / 2 + metrics.getAscent();
        graphics.drawString(text, textX, textY);
    }

    /**
     * Convierte un array de bytes de imagen QR a MultipartFile para subir a S3
     * 
     * @param qrImageBytes Array de bytes de la imagen QR
     * @param filename Nombre del archivo
     * @return MultipartFile para subir a S3
     */
    public MultipartFile createQrMultipartFile(byte[] qrImageBytes, String filename) {
        return new MultipartFile() {
            @Override
            public String getName() {
                return "qrImage";
            }

            @Override
            public String getOriginalFilename() {
                return filename;
            }

            @Override
            public String getContentType() {
                return "image/png";
            }

            @Override
            public boolean isEmpty() {
                return qrImageBytes == null || qrImageBytes.length == 0;
            }

            @Override
            public long getSize() {
                return qrImageBytes.length;
            }

            @Override
            public byte[] getBytes() {
                return qrImageBytes;
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(qrImageBytes);
            }

            @Override
            public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
                throw new UnsupportedOperationException("Transfer to file not supported");
            }
        };
    }
}
