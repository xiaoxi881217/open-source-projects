package com.kingzcheung.xime.ui.settings
internal fun homeInitialRoute(route:String?):String=when(route){
 "manage_dict"->SettingsRoutes.Dictionary
 "schema"->SettingsRoutes.Schema
 "plugins"->SettingsRoutes.Plugins
 "home_drafts"->SettingsRoutes.HomeDrafts
 "home_appearance"->SettingsRoutes.HomeAppearance
 "home_connection"->SettingsRoutes.HomeConnection
 else->SettingsRoutes.HomeHub
}
