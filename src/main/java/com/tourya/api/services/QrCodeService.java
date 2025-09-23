package com.tourya.api.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
     * Genera un código QR como array de bytes.
     * 
     * @param qrContent Contenido del código QR
     * @return Array de bytes de la imagen PNG
     * @throws WriterException si hay error al generar el QR
     * @throws IOException si hay error al convertir a bytes
     */
    public byte[] generateQrCodeImage(String qrContent) throws WriterException, IOException {
        log.debug("Generating QR code for content: {}", qrContent);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        
        // Configurar hints para el QR
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        // Generar la matriz del código QR
        BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT, hints);

        // Convertir a imagen PNG
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, IMAGE_FORMAT, outputStream);
        
        byte[] qrImageBytes = outputStream.toByteArray();
        outputStream.close();

        log.debug("Generated QR code with {} bytes", qrImageBytes.length);
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
}
