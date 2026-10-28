package com.kingzcheung.xime.appearance
import com.kingzcheung.xime.ui.settings.*
import org.junit.Assert.*
import org.junit.Test
class HomeNavigationPolicyTest {
 @Test fun ordinaryLaunchExposesHomeInsteadOfUpstreamSettings(){assertEquals(SettingsRoutes.HomeHub,homeInitialRoute(null));assertEquals(SettingsRoutes.HomeHub,homeInitialRoute("unknown"))}
 @Test fun appearanceAndDraftLinksRemainDirect(){assertEquals(SettingsRoutes.HomeAppearance,homeInitialRoute("home_appearance"));assertEquals(SettingsRoutes.HomeDrafts,homeInitialRoute("home_drafts"));assertEquals(SettingsRoutes.Schema,homeInitialRoute("schema"))}
}
