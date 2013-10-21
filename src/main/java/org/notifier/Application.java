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
import talkwut.core.Protocol;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;


public class Application {
    private static String twUserRegistrationQueue = "talkwut-register";
    private static String twUserName = "giko";

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

        String queueName = "talkwut-user-" + twUserName;
        channel.queueDeclare(queueName, false, false, true, null);


        Protocol.Registration registration = Protocol.Registration.newBuilder()
                .setQueue(queueName).setUser(twUserName).build();

        channel.queueDeclare(twUserRegistrationQueue, true, false, true, null);
        channel.basicPublish("", twUserRegistrationQueue, null, registration.toByteArray());

        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueName, true, consumer);

        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            final talkwut.notifier.Protocol.Notification notification = talkwut.notifier.Protocol.Notification.parseFrom(delivery.getBody());

            new NotificationBuilder()
                    .withStyle(style) // Required. here we set the previously set style
                    .withTitle(notification.getCategory()) // Required.
                    .withMessage(notification.getMessage()) // Optional
                    .withIcon(new ImageIcon(Application.class.getResource("/resources/pony.png"))) // Optional. You could also use a String path
                    .withDisplayTime(7000) // Optional
                    .withPosition(Positions.SOUTH_EAST) // Optional. Show it at the center of the screen
                    .withListener(new NotificationEventAdapter() { // Optional
                        public void closed(NotificationEvent event) {
                            System.out.println("closed notification with UUID " + event.getId());
                        }

                        public void clicked(NotificationEvent event) {
                            try {
                                Desktop.getDesktop().browse(URI.create(notification.getUrl()));
                            } catch (IOException e) {
                                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            }
                        }
                    })
                    .showNotification(); // this returns a UUID that you can use to identify events on the listener
        }
    }

}