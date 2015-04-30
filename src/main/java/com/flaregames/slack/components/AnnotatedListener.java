package com.flaregames.slack.components;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import in.ashwanthkumar.slack.webhook.Slack;
import in.ashwanthkumar.slack.webhook.SlackMessage;
import in.ashwanthkumar.slack.webhook.SlackAttachment;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.confluence.event.events.content.ContentEvent;
import com.atlassian.confluence.event.events.content.blogpost.BlogPostCreateEvent;
import com.atlassian.confluence.event.events.content.page.PageCreateEvent;
import com.atlassian.confluence.event.events.content.page.PageUpdateEvent;
import com.atlassian.confluence.event.events.content.comment.CommentEvent;
import com.atlassian.confluence.event.events.content.comment.CommentCreateEvent;
import com.atlassian.confluence.pages.Comment;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.pages.TinyUrl;
import com.atlassian.confluence.user.PersonalInformationManager;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceUrlProvider;
import com.atlassian.user.User;

public class AnnotatedListener implements DisposableBean, InitializingBean {
   private static final Logger              LOGGER = LoggerFactory.getLogger(AnnotatedListener.class);

   private final WebResourceUrlProvider     webResourceUrlProvider;
   private final EventPublisher             eventPublisher;
   private final ConfigurationManager       configurationManager;
   private final PersonalInformationManager personalInformationManager;
   private final UserAccessor               userAccessor;

   public AnnotatedListener(EventPublisher eventPublisher, ConfigurationManager configurationManager, PersonalInformationManager personalInformationManager,
         UserAccessor userAccessor, WebResourceUrlProvider webResourceUrlProvider) {
      this.eventPublisher = checkNotNull(eventPublisher);
      this.configurationManager = checkNotNull(configurationManager);
      this.userAccessor = checkNotNull(userAccessor);
      this.personalInformationManager = checkNotNull(personalInformationManager);
      this.webResourceUrlProvider = checkNotNull(webResourceUrlProvider);
   }

   @EventListener
   public void blogPostCreateEvent(BlogPostCreateEvent event) {
      sendMessages(event, event.getBlogPost(), "new blog post");
   }

   @EventListener
   public void pageCreateEvent(PageCreateEvent event) {
      sendMessages(event, event.getPage(), "new page created");
   }

   @EventListener
   public void pageUpdateEvent(PageUpdateEvent event) {
      sendMessages(event, event.getPage(), "page updated");
   }

   @EventListener
   public void commentCreateEvent(CommentCreateEvent event) {
      String maps = configurationManager.getMappedUsers();
      Comment com = event.getComment();
      AbstractPage page = com.getPage();
      List<String> auth = new ArrayList<String>();

      if(StringUtils.isBlank(maps) || !getCommentsenabled(page)) {
          return; }

      String creator = page.getCreator().getName().toLowerCase();
      String lastmodifier = page.getLastModifier().getName().toLowerCase();
      String commentator = com.getCreator().getName().toLowerCase();

      //check if current comment is answer to previus comment(s) and all involved authors
      Comment firstcom = com;
      while(firstcom.getParent() != null) { //get first in line
          firstcom = firstcom.getParent(); }
      List<String> descendants = new ArrayList<String>(firstcom.getDescendantAuthors());

      //Check if match with Slack User
      String[] mapLines = maps.split(System.getProperty("line.separator"));
      for (String couple : mapLines) {   //Test for MapList Match
          String[] cpl = couple.replaceAll("\\s","").split(",");
          if(commentator.equals(cpl[0])) { //no notifications to commentator himself
            continue; }

          if(!descendants.isEmpty()) { //Answer
              if(descendants.contains(cpl[0])) {
                  auth.add("@"+cpl[1]); }
          }
          else { //single comment
              if(creator.equals(cpl[0]) || lastmodifier.equals(cpl[0])) {
                  auth.add("@"+cpl[1]); }
          }
      }
      if(auth.isEmpty()) {
          return; }

      //Send comment notifications only to authers and not to channels
      String authors = StringUtils.join(auth, ',');
      String old = StringUtils.join(getChannels(page), ',');
      setChannels(page, authors);

      //Set contents
      String content = com.getBodyAsStringWithoutMarkup();
      String comPath = webResourceUrlProvider.getBaseUrl(UrlMode.ABSOLUTE) + "/" + page.getUrlPath() + "#comment-thread-"+com.getIdAsString();
      String text = "comment added";

      sendMessages(event, page, text, content, comPath, commentator);
      setChannels(page, old);
   }

   private void sendMessages(ContentEvent event, AbstractPage page, String action, String... commentContents) {
      if (event.isSuppressNotifications()) {
         LOGGER.info("Suppressing notification for {}.", page.getTitle());
         return;
      }

      Object message = null;
      if(commentContents.length > 0) { 
          Integer chars = Integer.parseInt(configurationManager.getMaxChars()); 
          if(commentContents[0].length() < chars) { //to short to turn into attachment
              message = getMessage(page, action, commentContents[0], commentContents[1], commentContents[2]); 
          }
          else {
              message = getAttachment(page, action, commentContents[0], commentContents[1], commentContents[2]); }
      }
      else { 
        message = getMessage(page, action); }

      for (String channel : getChannels(page)) { 
        sendMessage(channel, message); 
      }

   }

   private void setChannels(AbstractPage page, String channels) {
      String key = page.getSpaceKey();
      if(channels.isEmpty()) {
        return; }
      configurationManager.setSpaceChannels(key, channels);
   }
   private List<String> getChannels(AbstractPage page) {
      String spaceChannels = configurationManager.getSpaceChannels(page.getSpaceKey());
      if (spaceChannels.isEmpty()) {
         return Collections.emptyList();
      }
      return Arrays.asList(spaceChannels.split(","));
   }
   private Boolean getCommentsenabled(AbstractPage page) {
      return (configurationManager.getSpaceCommentsenabled(page.getSpaceKey()).equals("checked")) ? true : false;
   }

   private SlackAttachment getAttachment(AbstractPage page, String action, String attachment, String comPath, String commentator) { //attachments are always comments
      String commentatorname = (commentator.length() >= 1) ? (commentator.substring(0, 1).toUpperCase() + commentator.substring(1)) : commentator;
      User user = userAccessor.getUser(commentatorname);
      
      SlackAttachment message = new SlackAttachment(attachment);
      message = message.color("#205081");

      message = message.title(page.getSpace().getDisplayTitle() + " - " + page.getTitle(), tinyLink(page));
      message = (null == user) ? message.preText(action) : message.preText(action);

      String comFormatPath = "<"+comPath+"|#comment>";
      SlackAttachment.Field comLink = new SlackAttachment.Field("Direct Link", comFormatPath, true);
      message = message.addField(comLink);

      String authorPath = webResourceUrlProvider.getBaseUrl(UrlMode.ABSOLUTE) + "/" + personalInformationManager.getOrCreatePersonalInformation(user).getUrlPath();
      message = message.author("Author: "+user.getFullName(), authorPath);

      return message.fallback(page.getSpace().getDisplayTitle() + " - " + page.getTitle()+" - " + action);
   }

   private SlackMessage getMessage(AbstractPage page, String action, String... commentContents) {
      String username = isNullOrEmpty(page.getLastModifierName()) ? page.getCreatorName() : page.getLastModifierName();
      SlackMessage message = new SlackMessage();

      message = appendPageLink(message, page);
      if(commentContents.length > 0) { //check if comment
        message = message.text(" - <" + commentContents[1] + "|#comment> added"+ commentContents[0] +" by "); 
        username = (commentContents[2].length() >= 1) ? (commentContents[2].substring(0, 1).toUpperCase() + commentContents[2].substring(1)) : commentContents[2];
      }
      else {
          message = message.text(" - " + action + " by "); }

      User user = userAccessor.getUser(username);
      return appendPersonalSpaceUrl(message, user);
   }

   private void sendMessage(String channel, Object message) {
      LOGGER.info("Sending to {} on channel {} with message {}.", configurationManager.getWebhookUrl(), channel, message.toString());
      try {
          if(channel.indexOf("@") == 0) {
              String usr = channel.substring(1);
              if(message instanceof SlackMessage) {
                  SlackMessage msg = (SlackMessage)message;
                  new Slack(configurationManager.getWebhookUrl()).displayName("Confluence").sendToUser(usr).push(msg);
              }
              else {
                  SlackAttachment msg = (SlackAttachment)message;
                  new Slack(configurationManager.getWebhookUrl()).displayName("Confluence").sendToUser(usr).push(msg);
              }
          }
          else {
              if(message instanceof SlackMessage) {
                  SlackMessage msg = (SlackMessage)message;
                  new Slack(configurationManager.getWebhookUrl()).displayName("Confluence").sendToChannel(channel).push(msg);
              }
              else {
                  SlackAttachment msg = (SlackAttachment)message;
                  new Slack(configurationManager.getWebhookUrl()).displayName("Confluence").sendToChannel(channel).push(msg);
              }
          }
      }
      catch (IOException e) {
         LOGGER.error("Error when sending Slack message", e);
      }
   }

   private SlackMessage appendPersonalSpaceUrl(SlackMessage message, User user) {
      if (null == user) {
         return message.text("unknown user");
      }
      return message.link(webResourceUrlProvider.getBaseUrl(UrlMode.ABSOLUTE) + "/" + personalInformationManager.getOrCreatePersonalInformation(user).getUrlPath(), user.getFullName());
   }

   private SlackMessage appendPageLink(SlackMessage message, AbstractPage page) {
      return message.link(tinyLink(page), page.getSpace().getDisplayTitle() + " - " + page.getTitle());
   }

   private String tinyLink(AbstractPage page) {
      return webResourceUrlProvider.getBaseUrl(UrlMode.ABSOLUTE) + "/x/" + new TinyUrl(page).getIdentifier();
   }

   @Override
   public void afterPropertiesSet() throws Exception {
      LOGGER.debug("Register Slack event listener");
      eventPublisher.register(this);
   }

   @Override
   public void destroy() throws Exception {
      LOGGER.debug("Un-register Slack event listener");
      eventPublisher.unregister(this);
   }
}
