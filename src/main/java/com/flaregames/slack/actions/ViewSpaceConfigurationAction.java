package com.flaregames.slack.actions;

import com.atlassian.confluence.spaces.actions.AbstractSpaceAdminAction;
import com.flaregames.slack.components.ConfigurationManager;
import com.opensymphony.xwork.Action;

public final class ViewSpaceConfigurationAction extends AbstractSpaceAdminAction {
   private static final long          serialVersionUID = 5691912273454934901L;

   private final ConfigurationManager configurationManager;
   private String                     channels;
   private String                     commentsenabled;
   private boolean                    successFullUpdate;

   public ViewSpaceConfigurationAction(ConfigurationManager configurationManager) {
      this.configurationManager = configurationManager;
   }

   public void setResult(String result) {
      if ("success".equals(result)) {
         successFullUpdate = true;
      }
   }

   @Override
   public String execute() {
      setChannels(configurationManager.getSpaceChannels(key));
      setCommentsenabled(configurationManager.getSpaceCommentsenabled(key));
      return Action.SUCCESS;
   }

   public void setChannels(String channels) {
      this.channels = channels;
   }
   public String getChannels() {
      return channels;
   }

   public void setCommentsenabled(String commessages) {
      this.commentsenabled = commessages;
   }
   public String getCommentsenabled() {
      return commentsenabled;
   }

   public boolean isSuccessFullUpdate() {
      return successFullUpdate;
   }
}
