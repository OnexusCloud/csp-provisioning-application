package net.respectnetwork.csp.application.invite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import net.respectnetwork.csp.application.dao.DAOContextProvider;
import net.respectnetwork.sdk.csp.notification.BasicNotificationService;
import net.respectnetwork.sdk.csp.notification.NotificationException;

public class GiftEmailSenderThread implements Runnable
{
   private String subject = "";
   private String toAddress = "";
   private String content = "";

   private static final Logger logger = LoggerFactory.getLogger(GiftEmailSenderThread.class);
   @Override
   public void run()
   {
      BasicNotificationService svc = (BasicNotificationService) DAOContextProvider.getApplicationContext().getBean("basicNotifier");
      
      svc.setEmailSubject(subject);
      try
      {
         svc.sendEmailNotification(toAddress, content);
      } catch (NotificationException e)
      {
         logger.error("Could not send mail to " + toAddress + " , with content \n" + content);
         logger.error("Mail exception " + e.getMessage());
      }

   }
   public String getSubject()
   {
      return subject;
   }
   public void setSubject(String subject)
   {
      this.subject = subject;
   }
   public String getToAddress()
   {
      return toAddress;
   }
   public void setToAddress(String toAddress)
   {
      this.toAddress = toAddress;
   }
   public String getContent()
   {
      return content;
   }
   public void setContent(String content)
   {
      this.content = content;
   }

}
