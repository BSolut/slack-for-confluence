package com.flaregames.slack.actions;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.confluence.security.SpacePermission;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.xwork.RequireSecurityToken;
import com.flaregames.slack.components.ConfigurationManager;
import com.opensymphony.xwork.Action;

public class SaveSpaceConfigurationAction extends ConfluenceActionSupport {
   private static final long    serialVersionUID = -3368277537107958205L;

   private ConfigurationManager configurationManager;
   private SpaceManager         spaceManager;

   private String               key;
   private String               channels;
   private String               commessages;

   @Override
   public void validate() {
      super.validate();

      if (StringUtils.isBlank(key) || spaceManager.getSpace(key) == null) {
         addActionError(getText("slack.spaceconfig.spacekeyerror"));
      }
   }

   @Override
   public boolean isPermitted() {
      return spacePermissionManager.hasPermissionForSpace(getAuthenticatedUser(), Arrays.asList(SpacePermission.ADMINISTER_SPACE_PERMISSION), spaceManager.getSpace(key));
   }

   @Override
   @RequireSecurityToken(true)
   public String execute() throws Exception {
      configurationManager.setSpaceChannels(key, channels);
      configurationManager.setSpaceComsEnabled(key, commessages);
      return Action.SUCCESS;
   }

   public String getKey() {
      return key;
   }

   public void setKey(String key) {
      this.key = key;
   }

   public String getChannels() {
      return channels;
   }
   public void setChannels(String channels) {
      this.channels = channels;
   }

   public String getComsEnabled() {
      return commessages;
   }
   public void setComsEnabled(String commessages) {
      this.commessages = commessages;
   }

   public void setConfigurationManager(ConfigurationManager configurationManager) {
      this.configurationManager = configurationManager;
   }

   public void setSpaceManager(SpaceManager spaceManager) {
      this.spaceManager = spaceManager;
   }
}
