/*
 * ProfilerEditingTargetWidget.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.profiler;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.theme.ThemeColors;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.model.PublishHtmlSource;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishButton;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.PanelWithToolbars;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetToolbar;

public class ProfilerEditingTargetWidget extends Composite
                                         implements ProfilerPresenter.Display
              
{
   private RStudioFrame profilePage_;
   
   public ProfilerEditingTargetWidget(Commands commands, PublishHtmlSource publishHtmlSource)
   {
      VerticalPanel panel = new VerticalPanel();


      PanelWithToolbars mainPanel = new PanelWithToolbars(
                                          createToolbar(commands, publishHtmlSource), 
                                          panel);

      profilePage_ = new RStudioFrame();
      profilePage_.setAceThemeAndCustomStyle(
         getCustomStyle(),
         "../profiler_resource/profiler.css");

      profilePage_.setWidth("100%");
      profilePage_.setHeight("100%");
      
      panel.add(profilePage_);
      panel.setWidth("100%");
      panel.setHeight("100%");
      
      // needed for Firefox
      if (BrowseCap.isFirefox())
         profilePage_.getElement().getParentElement().setAttribute("height", "100%");
      
      initWidget(mainPanel);
   }

   private String getCustomStyle()
   {
      return
         ".rstudio-themes-flat.rstudio-themes-default .profvis-footer,\n" +
         ".rstudio-themes-flat.rstudio-themes-default .info-block {\n" +
         "   border-color: " + ThemeColors.defaultBorder + ";\n" +
         "}\n" +
         "\n" +
         ".rstudio-themes-flat.rstudio-themes-dark-grey .profvis-footer,\n" +
         ".rstudio-themes-flat.rstudio-themes-dark-grey .info-block {\n" +
         "   border-color: " + ThemeColors.darkGreyBorder + ";\n" +
         "}\n" +
         "\n" +
         ".rstudio-themes-flat.rstudio-themes-alternate .profvis-footer,\n" +
         ".rstudio-themes-flat.rstudio-themes-alternate .info-block {\n" +
         "   border-color: " + ThemeColors.alternateBorder + ";\n" +
         "}\n" +
         "\n" +
         ".rstudio-themes-flat.rstudio-themes-default .profvis-footer,\n" +
         ".rstudio-themes-flat.rstudio-themes-default .profvis-status-bar {\n" +
         "   background-color: " + ThemeColors.defaultBackground + ";\n" +
         "}\n" +
         "\n" +
         ".rstudio-themes-flat.rstudio-themes-dark-grey .profvis-footer,\n" +
         ".rstudio-themes-flat.rstudio-themes-dark-grey .profvis-status-bar {\n" +
         "   background-color: " + ThemeColors.darkGreyBackground + ";\n" +
         "}\n" +
         "\n" +
         ".rstudio-themes-flat.rstudio-themes-alternate .profvis-footer,\n" +
         ".rstudio-themes-flat.rstudio-themes-alternate .profvis-status-bar {\n" +
         "   background-color: " + ThemeColors.alternateBackground + ";\n" +
         "}\n" +
         "\n" +
         ".rstudio-themes-flat.rstudio-themes-default .profvis-splitbar-horizontal {\n" +
         "   background-color: " + ThemeColors.defaultBodyBackground + ";\n" +
         "}\n" +
         "\n" +
         ".rstudio-themes-flat.rstudio-themes-dark-grey .profvis-splitbar-horizontal {\n" +
         "   background-color: " + ThemeColors.darkGreyBodyBackground + ";\n" +
         "}\n" +
         "\n" +
         ".rstudio-themes-flat.rstudio-themes-alternate .profvis-splitbar-horizontal {\n" +
         "   background-color: " + ThemeColors.alternateBodyBackground + ";\n" +
         "}\n" +
         "\n" +
         ".rstudio-themes-flat.rstudio-themes-default .result-block-active {\n" +
         "   background-color: " + ThemeColors.defaultMostInactiveBackground + ";\n" +
         "}\n" +
         "\n" +
         ".rstudio-themes-flat.rstudio-themes-dark-grey .result-block-active {\n" +
         "   background-color: " + ThemeColors.darkGreyMostInactiveBackground + ";\n" +
         "}\n" +
         "\n" +
         ".rstudio-themes-flat.rstudio-themes-alternate .result-block-active {\n" +
         "   background-color: " + ThemeColors.alternateMostInactiveBackground + ";\n" +
         "}\n";
   }

   public void print()
   {
      WindowEx window = profilePage_.getWindow();
      window.focus();
      window.print();
   }

   private Toolbar createToolbar(Commands commands, PublishHtmlSource publishHtmlSource)
   {
      Toolbar toolbar = new EditingTargetToolbar(commands, true);
      
      toolbar.addLeftWidget(commands.gotoProfileSource().createToolbarButton());
      toolbar.addLeftWidget(commands.saveProfileAs().createToolbarButton());
      
      toolbar.addRightWidget(
            publishButton_ = new RSConnectPublishButton(
                  RSConnect.CONTENT_TYPE_DOCUMENT, true, null));
      
      publishButton_.setPublishHtmlSource(publishHtmlSource);
      publishButton_.setContentType(RSConnect.CONTENT_TYPE_HTML);
      
      return toolbar;
   }
   
   public Widget asWidget()
   {
      return this;
   }
   
   public void showProfilePage(String path)
   {
      profilePage_.setUrl(path);
   }
   
   public String getUrl()
   {
      return profilePage_.getUrl();
   }
   
   private RSConnectPublishButton publishButton_;
}
