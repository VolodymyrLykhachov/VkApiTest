package ua.vladimir.lykhachov;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

//import org.apache.http.impl.client.DefaultHttpClient;


public class VKapi {
    private String client_id = "4833083";
    private String scope = "messages";
    private String redirect_uri = "http://oauth.vk.com/blank.html";
    private String display = "popup";
    private String response_type = "token";
    private String access_token;
    private String email = "v_lykhachov@mail.ru";//тут должен быть прописан email
    private String pass = "Vfe'hkfn";//тут должен быть прописан пароль

    public void setConnection() throws IOException, URISyntaxException {
        //Код получения token'a
        //HttpClient httpClient = new DefaultHttpClient();
        HttpClient httpClient = HttpClientBuilder.create().build();
        // Делаем первый запрос
        HttpPost post = new HttpPost("http://oauth.vk.com/authorize?" +
                "client_id=" + client_id +
                "&scope=" + scope +
                "&redirect_uri=" + redirect_uri +
                "&display=" + display +
                "&response_type=" + response_type);
        HttpResponse response;
        response = httpClient.execute(post);
        post.abort();
        //Получаем редирект
        String HeaderLocation = response.getFirstHeader("location").getValue();
        URI RedirectUri = new URI(HeaderLocation);
        //Для запроса авторизации необходимо два параметра полученных в первом запросе
        //ip_h и to_h
        String ip_h = RedirectUri.getQuery().split("&")[2].split("=")[1];
        String to_h = RedirectUri.getQuery().split("&")[4].split("=")[1];
        // Делаем запрос авторизации
        post = new HttpPost("https://login.vk.com/?act=login&soft=1" +
                "&q=1" +
                "&ip_h=" + ip_h +
                "&from_host=oauth.vk.com" +
                "&to=" + to_h +
                "&expire=0" +
                "&email=" + email +
                "&pass=" + pass);
        response = httpClient.execute(post);
        post.abort();
        // Получили редирект на подтверждение требований приложения
        HeaderLocation = response.getFirstHeader("location").getValue();
        post = new HttpPost(HeaderLocation);
        // Проходим по нему
        response = httpClient.execute(post);
        post.abort();
        // Теперь последний редирект на получение токена
        HeaderLocation = response.getFirstHeader("location").getValue();
        // Проходим по нему
        post = new HttpPost(HeaderLocation);
        response = httpClient.execute(post);
        post.abort();
        // Теперь в след редиректе необходимый токен
        HeaderLocation = response.getFirstHeader("location").getValue();
        // Просто спарсим его сплитами
        access_token = HeaderLocation.split("#")[1].split("&")[0].split("=")[1];
    }

    public String getNewMessage() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        //Ранее описанный код получения списка сообщений
        //формируем строку запроса
        String url = "https://api.vk.com/method/" +
                "messages.get" +
                "?out=0" +
                "&access_token=" + access_token;
        String line = "";
        try {
            URL url2 = new URL(url);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url2.openStream()));
            line = reader.readLine();
            reader.close();

        } catch (MalformedURLException e) {
            // ...
        } catch (IOException e) {
            // ...
        }
        return line;
    }

    public static void main(String[] args) throws IOException, URISyntaxException, AWTException, InterruptedException, NoSuchAlgorithmException {
        //Создадим раскрывающееся меню
        PopupMenu popup = new PopupMenu();
        //Создадим элемент меню
        MenuItem exitItem = new MenuItem("Выход");
        //Добавим для него обработчик
        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        //Добавим пункт в меню
        popup.add(exitItem);
        SystemTray systemTray = SystemTray.getSystemTray();
        //получим картинку
        Image image = Toolkit.getDefaultToolkit().getImage("vk_icon.png");
        TrayIcon trayIcon = new TrayIcon(image, "VKNotifer", popup);
        trayIcon.setImageAutoSize(true);
        //добавим иконку в трей
        systemTray.add(trayIcon);
        trayIcon.displayMessage("VKNotifer", "Соединяемся с сервером", TrayIcon.MessageType.INFO);
        //Создадим экземпляр класса ВКапи
        VKapi vkAPI = new VKapi();
        //Получим токен
        vkAPI.setConnection();
        trayIcon.displayMessage("VKNotifer", "Соединение установлено", TrayIcon.MessageType.INFO);
        //Бескоечный цикл
        String oldMessage = vkAPI.getNewMessage();
        String newMessage;
        int i = 0;
        for (; ; ) {
            // Запросы на сервер можно подавать раз в 3 секунды
            Thread.sleep(3000); // ждем три секунды
            if (i == 15000) {  // Если прошло 45 000 сек (Время взято с запасом, токен дается на день )
                vkAPI.setConnection(); // Обновляем токен
                Thread.sleep(3000);    // Запросы шлем только раз в три секунды
                i = 0;
            }
            //Здесь отработка
            newMessage = vkAPI.getNewMessage();
            if (!newMessage.equals(oldMessage)) {
                oldMessage = newMessage;
                trayIcon.displayMessage("VKNotifer", "Получено новое сообщение", TrayIcon.MessageType.INFO);
            }
            i++;
        }
    }
}
