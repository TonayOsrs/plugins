package net.runelite.client.plugins.oneclickhousetabs;

import javax.inject.Inject;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.queries.GameObjectQuery;
import net.runelite.api.queries.NPCQuery;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.pf4j.Extension;

import java.util.*;

@Extension
@PluginDescriptor(
        name = "One Click House Tabs",
        enabledByDefault = false,
        description = "Makes House tabs and unnotes at phial."
)
@Slf4j
public class OneClickHouseTabsPlugin extends Plugin {
    private int timeout;

    @Inject
    private Client client;

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (timeout>0) timeout--;
        if (client.getLocalPlayer()!=null && client.getLocalPlayer().getAnimation()==4067) timeout = 6;
        if (getInventoryItem(ItemID.SOFT_CLAY)==null) timeout = 0;
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) throws InterruptedException {
        if (event.getMenuOption().equals("<col=00ff00>One Click House Tabs"))
            handleClick(event);
    }

    @Subscribe
    private void onClientTick(ClientTick event) {
        if (this.client.getLocalPlayer() == null || this.client.getGameState() != GameState.LOGGED_IN) return;
        String text = "<col=00ff00>One Click House Tabs";
        this.client.insertMenuItem(text, "", MenuAction.UNKNOWN.getId(), 0, 0, 0, true);
        client.setTempMenuEntry(Arrays.stream(client.getMenuEntries()).filter(x->x.getOption().equals(text)).findFirst().orElse(null));
    }

    private void handleClick(MenuOptionClicked event) {
        if (timeout>0)
        {
            event.consume();
            return;
        }
        if((client.getLocalPlayer()!=null && client.getLocalPlayer().isMoving() || client.getLocalPlayer().getPoseAnimation() != client.getLocalPlayer().getIdlePoseAnimation())
            && client.getWidget(219,1)==null)
        {
            event.consume();
            return;
        }

        if (inPOH())
        {
            if (tabMenuOpen())
            {
                event.setMenuEntry(createTabsMES());
                return;
            }

            if (getInventoryItem(ItemID.SOFT_CLAY)!=null && clickLecternMES()!=null)
            {
                event.setMenuEntry(clickLecternMES());
                return;
            }
            event.setMenuEntry(leavePOHMES());
            return;
        }
        if (getInventoryItem(ItemID.SOFT_CLAY)!=null && enterPOHMES()!=null)
        {
            event.setMenuEntry(enterPOHMES());
            return;
        }
        if (client.getWidget(219,1)!=null
                && client.getWidget(219,1).getChild(3)!=null)
        {
            event.setMenuEntry(exchangeAllMES());
            return;
        }
        if (getInventoryItem(1762)!=null && useNotedClayOnPhialsMES()!=null && getInventoryItem(ItemID.SOFT_CLAY)==null)
        {
            event.setMenuEntry(useNotedClayOnPhialsMES());
        }
    }

    private Widget getInventoryItem(int id) {
        Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
        Widget bankInventoryWidget = client.getWidget(WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER);
        if (inventoryWidget!=null && !inventoryWidget.isHidden())
        {
            return getWidgetItem(inventoryWidget,id);
        }
        if (bankInventoryWidget!=null && !bankInventoryWidget.isHidden())
        {
            return getWidgetItem(bankInventoryWidget,id);
        }
        return null;
    }

    private Widget getWidgetItem(Widget widget,int id) {
        for (Widget item : widget.getDynamicChildren())
        {
            if (item.getItemId() == id)
            {
                return item;
            }
        }
        return null;
    }
    private Point getLocation(TileObject tileObject) {
        if (tileObject == null) {
            return new Point(0, 0);
        }
        if (tileObject instanceof GameObject) {
            return ((GameObject) tileObject).getSceneMinLocation();
        }
        return new Point(tileObject.getLocalLocation().getSceneX(), tileObject.getLocalLocation().getSceneY());
    }

    private Point getLocation(NPC npc)
    {
        return new Point(npc.getLocalLocation().getSceneX(),npc.getLocalLocation().getSceneY());
    }

    private GameObject getGameObject(int ID) {
        return new GameObjectQuery()
                .idEquals(ID)
                .result(client)
                .nearestTo(client.getLocalPlayer());
    }

    private NPC getNpc(int... id)
    {
        return new NPCQuery()
                .idEquals(id)
                .result(client)
                .nearestTo(client.getLocalPlayer());
    }

    private boolean inPOH() {
        return getGameObject(ObjectID.PORTAL_4525)!=null;
    }

    private boolean tabMenuOpen() {
        return client.getWidget(79,15)!=null;
    }

    private MenuEntry useNotedClayOnPhialsMES() {
        int SOFT_CLAY = 1762;
        client.setSelectedSpellWidget(WidgetInfo.INVENTORY.getId());
        client.setSelectedSpellChildIndex(getInventoryItem(SOFT_CLAY).getIndex());
        client.setSelectedSpellItemId(SOFT_CLAY);
        NPC phials = getNpc(NpcID.PHIALS);
        if (phials == null) return null;
        return createMenuEntry(phials.getIndex(), MenuAction.WIDGET_TARGET_ON_NPC, getLocation(phials).getX(), getLocation(phials).getY(), false);
    }

    private MenuEntry exchangeAllMES() {
        return createMenuEntry(0, MenuAction.WIDGET_CONTINUE, 3, WidgetInfo.DIALOG_OPTION_OPTION1.getId(), false);
    }

    private MenuEntry enterPOHMES() {
        GameObject housePortal = getGameObject(ObjectID.PORTAL_15478);
        if (housePortal== null) return null;
        return createMenuEntry(housePortal.getId(), MenuAction.GAME_OBJECT_SECOND_OPTION, getLocation(housePortal).getX(), getLocation(housePortal).getY(), false);
    }

    private MenuEntry clickLecternMES() {
        GameObject lectern = getGameObject(ObjectID.LECTERN_37349);
        if (lectern== null) return null;
        return createMenuEntry(lectern.getId(), MenuAction.GAME_OBJECT_FIRST_OPTION, getLocation(lectern).getX(), getLocation(lectern).getY(), false);
    }

    private MenuEntry createTabsMES() {
        return createMenuEntry(1, MenuAction.CC_OP, -1, 5177359, false);
    }

    private MenuEntry leavePOHMES() {
        GameObject portal = getGameObject(ObjectID.PORTAL_4525);
        return createMenuEntry(portal.getId(),MenuAction.GAME_OBJECT_FIRST_OPTION, getLocation(portal).getX(), getLocation(portal).getY(), false);
    }

    public MenuEntry createMenuEntry(int identifier, MenuAction type, int param0, int param1, boolean forceLeftClick) {
        return client.createMenuEntry(0).setOption("").setTarget("").setIdentifier(identifier).setType(type)
                .setParam0(param0).setParam1(param1).setForceLeftClick(forceLeftClick);
    }
}