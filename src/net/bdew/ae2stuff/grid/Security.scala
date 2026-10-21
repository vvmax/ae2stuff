/*
 * Copyright (c) bdew, 2014 - 2015
 * https://github.com/bdew/ae2stuff
 *
 * This mod is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://bdew.net/minecraft-mod-public-license/
 */

package net.bdew.ae2stuff.grid

import appeng.api.AEApi
import appeng.api.config.SecurityPermissions
import appeng.api.networking.security.ISecurityGrid
import appeng.api.networking.{IGrid, IGridNode}
import com.mojang.authlib.GameProfile
import net.minecraft.entity.player.EntityPlayer

object Security {
  def getPlayerId(p: GameProfile): Int = AEApi.instance().registries().players().getID(p)
  def getPlayerId(e: EntityPlayer): Int = getPlayerId(e.getGameProfile)

  def getSecurityId(n: IGridNode): Long = {
    // Not exposed by IGridNode: rv3 has a getter, rv2 has a public field.
    try {
      n.getClass.getMethod("getLastSecurityKey").invoke(n).asInstanceOf[Long]
    } catch {
      case _: NoSuchMethodException => n.getClass.getField("lastSecurityKey").getLong(n)
    }
  }

  def playerHasPermission(grid: IGrid, playerID: Int, permission: SecurityPermissions): Boolean = {
    if (grid == null) return true
    val gs = grid.getCache[ISecurityGrid](classOf[ISecurityGrid])
    if (gs == null || !gs.isAvailable) return true
    gs.hasPermission(playerID, permission)
  }

  def isNodeOnSecureNetwork(n: IGridNode): Boolean = {
    if (n.getGrid == null) return false
    val gs = n.getGrid.getCache[ISecurityGrid](classOf[ISecurityGrid])
    if (gs == null) return false
    gs.isAvailable
  }

  def canConnect(node1: IGridNode, node2: IGridNode): Boolean = {
    if (getSecurityId(node1) == getSecurityId(node2)) return true

    val ns1 = isNodeOnSecureNetwork(node1)
    val ns2 = isNodeOnSecureNetwork(node2)

    if (ns1 && ns2)
      false
    else if (ns1)
      playerHasPermission(node1.getGrid, node2.getPlayerID, SecurityPermissions.BUILD)
    else if (ns2)
      playerHasPermission(node2.getGrid, node1.getPlayerID, SecurityPermissions.BUILD)
    else
      true
  }
}
