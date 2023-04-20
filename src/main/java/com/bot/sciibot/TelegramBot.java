package com.bot.sciibot;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;

//Todo перекинуть консатны в пропертя
@Slf4j
@Component
@AllArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {
        return "ASCIITastuxBot";
    }

    @Override
    public String getBotToken() {
        return "6191114772:AAHOV4rpjJeOyUqpHf6jKPylsTxy54xlOAw";
    }

    @Override
    public void onRegister() {

    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            //Обработка тестовых команд
            switch (messageText) {
                case "/start":
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
            }
        }

        if (update.hasMessage() && update.getMessage().hasPhoto()) {
            final int percent = 9;

            // Получаем фото из сообщения
            List<PhotoSize> photos = update.getMessage().getPhoto();
            long chatId = update.getMessage().getChatId();
            // Получаем фото наибольшего размера
            PhotoSize photo = photos.stream()
                    .max(Comparator.comparing(PhotoSize::getFileSize))
                    .orElse(null);
            if (photo != null) {
                GetFile getFile = new GetFile();
                getFile.setFileId(photo.getFileId());
                File file;
                try {
                    file = execute(getFile);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                    return;
                }

                String path = "https://api.telegram.org/file/bot" + getBotToken() + "/" + file.getFilePath();

                try {
                    for (int i = 100; i > 0; i--) {
                        BufferedImage image = ImageIO.read(new URL(path));
                        String ascii = convertImageToAscii(resize(image, i));

                        log.atInfo().log(String.valueOf(ascii.length()));

                        if (ascii.length() <= 4096) {
                            sendMessage(chatId, ascii);
                            break;
                        }
                    }
                } catch (IOException e) {
                    log.info(e.getMessage());
                }
            }
        }
    }

    private void startCommandReceived(Long chatId, String name) {
        String answer = "Здорово, " + name + " я с тобой в благородство играть не буду, загрузишь мне пару изображений...";
        sendMessage(chatId, answer);
    }

    private void sendMessage(Long chatId, String textToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Метод, который конвертирует изображение в ASCll графику
    private String convertImageToAscii(BufferedImage image) {
        StringBuilder sb = new StringBuilder();

        int width = image.getWidth();
        int height = image.getHeight();
        for (int y = 0; y < height; y += 2) {
            for (int x = 0; x < width; x++) {
                Color pixelColor = new Color(image.getRGB(x, y));
                double brightness = getBrightness(pixelColor);
                sb.append(getAsciiChar(brightness));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // Метод, который определяет яркость пикселя
    private double getBrightness(Color pixelColor) {
        double brightness = 0.299 * pixelColor.getRed() + 0.587 * pixelColor.getGreen() + 0.114 * pixelColor.getBlue();
        return brightness / 255;
    }

    // Метод, который определяет символ ASCll, соответствующий яркости пикселя
    private char getAsciiChar(double brightness) {
        final char[] ASCiiToUse = {' ', '.', ':', '-', '=', '+', '*', '#', '%', '@'};

        int index = (int) (((double) ASCiiToUse.length - 1) * brightness);
        return ASCiiToUse[index];
    }

    //Метод компресси входного изображения
    public static BufferedImage resize(BufferedImage image, int percent) {
        int newWidth = (int) (image.getWidth() * percent / 100.0);
        int newHeight = (int) (image.getHeight() * percent / 100.0);
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, image.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return resizedImage;
    }
}
