package org.notifier;

import ch.swingfx.twinkle.NotificationBuilder;
import ch.swingfx.twinkle.event.NotificationEvent;
import ch.swingfx.twinkle.event.NotificationEventAdapter;
import ch.swingfx.twinkle.style.INotificationStyle;
import ch.swingfx.twinkle.style.theme.DarkDefaultNotification;
import ch.swingfx.twinkle.window.Positions;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class Application {
    private final static String EXCHANGE_NAME = "notifier";

    public static void main(String[] args) throws IOException, InterruptedException {
        // AA the text
        System.setProperty("swing.aatext", "true");

        // First we define the style/theme of the window.
        // Note how we override the default values
        INotificationStyle style = new DarkDefaultNotification()
                .withWidth(400) // Optional
                .withAlpha(0.9f) // Optional
                ;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("192.168.9.118");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "");

        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueName, true, consumer);

        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String message = new String(delivery.getBody());

            new NotificationBuilder()
                    .withStyle(style) // Required. here we set the previously set style
                    .withTitle("NRIV") // Required.
                    .withMessage(message) // Optional
                    .withIcon(new ImageIcon(Application.class.getResource("/resources/pony.png"))) // Optional. You could also use a String path
                    .withDisplayTime(7000) // Optional
                    .withPosition(Positions.SOUTH_EAST) // Optional. Show it at the center of the screen
                    .withListener(new NotificationEventAdapter() { // Optional
                        public void closed(NotificationEvent event) {
                            System.out.println("closed notification with UUID " + event.getId());
                        }

                        public void clicked(NotificationEvent event) {
                            try {
                                Desktop.getDesktop().browse(URI.create("http://192.168.24.169:8001/nrivapp/com.order/1"));
                            } catch (IOException e) {
                                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            }
                        }
                    })
                    .showNotification(); // this returns a UUID that you can use to identify events on the listener

        }

    }

}