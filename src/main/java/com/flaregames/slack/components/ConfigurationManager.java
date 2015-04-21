package com.flaregames.slack.components;

import com.atlassian.bandana.BandanaContext;
import com.atlassian.bandana.BandanaManager;
import com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext;

public class ConfigurationManager {
   private static final ConfluenceBandanaContext GLOBAL_CONTEXT = ConfluenceBandanaContext.GLOBAL_CONTEXT;
   private static final ConfluenceBandanaContext MAP_CONTEXT = ConfluenceBandanaContext.GLOBAL_CONTEXT;
   private BandanaManager                        bandanaManager;

   public ConfigurationManager(BandanaManager bandanaManager) {
      this.bandanaManager = bandanaManager;
   }

   //Webhook
   public String getWebhookUrl() {
      return getGlobalValue(ConfigurationOption.WEBHOOK_URL);
   }

   private String getGlobalValue(ConfigurationOption option) {
      return getBandanaValue(GLOBAL_CONTEXT, option);
   }

   public void setWebhookUrl(String webhookUrl) {
      setGlobalValue(ConfigurationOption.WEBHOOK_URL, webhookUrl);
   }

   private void setGlobalValue(ConfigurationOption option, String webhookUrl) {
      bandanaManager.setValue(GLOBAL_CONTEXT, option.getBandanaKey(), webhookUrl);
   }

   //Mapped Users
   public String getMappedUsers() {
      return getGlobalMappedValue(ConfigurationOption.MAPPED);
   }
   public String getMaxChars() {
      return getGlobalMappedValue(ConfigurationOption.MAXCHARS);
   }
   private String getGlobalMappedValue(ConfigurationOption option) {
      return getBandanaValue(MAP_CONTEXT, option);
   }

   public void setMappedUsers(String mappedUsers) {
      setGlobalMapped(ConfigurationOption.MAPPED, mappedUsers);
   }
   public void setMaxChars(String maxChars) {
      setGlobalMapped(ConfigurationOption.MAXCHARS, maxChars);
   }
   private void setGlobalMapped(ConfigurationOption option, String mappedUsers) {
      bandanaManager.setValue(MAP_CONTEXT, option.getBandanaKey(), mappedUsers);
   }
   
   //Channels
   public void setSpaceChannels(String spaceKey, String channels) {
      bandanaManager.setValue(new ConfluenceBandanaContext(spaceKey), ConfigurationOption.CHANNELS.getBandanaKey(), channels);
   }

   public String getSpaceChannels(String spaceKey) {
      return getBandanaValue(new ConfluenceBandanaContext(spaceKey), ConfigurationOption.CHANNELS);
   }

   private String getBandanaValue(BandanaContext bandanaContext, ConfigurationOption configurationOption) {
      Object fromBandana = bandanaManager.getValue(bandanaContext, configurationOption.getBandanaKey());
      if (fromBandana == null) {
         return "";
      }
      return fromBandana.toString();
   }

}
