package dev.wahaha.module.modules.player;

import dev.wahaha.Client;
import dev.wahaha.event.EventTarget;
import dev.wahaha.event.world.EventMotion;
import dev.wahaha.event.world.EventMove;
import dev.wahaha.event.world.EventPacketReceive;
import dev.wahaha.event.world.EventPacketSend;
import dev.wahaha.event.world.EventTick;
import dev.wahaha.event.world.EventUpdate;
import dev.wahaha.event.world.EventWorldLoad;
import dev.wahaha.module.Category;
import dev.wahaha.module.Module;
import dev.wahaha.module.modules.combat.KillAura;
import dev.wahaha.module.modules.combat.Velocity;
import dev.wahaha.module.modules.misc.Disabler;
import dev.wahaha.module.modules.movement.NoSlow;
import dev.wahaha.module.values.BoolValue;
import dev.wahaha.utils.DebugUtil;
import dev.wahaha.utils.InventoryUtil;
import dev.wahaha.utils.client.PacketUtil;
import dev.wahaha.utils.client.TimeUtil;
import java.util.ArrayList;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemAppleGold;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class Gapple extends Module {
    public static Velocity INSTANCE;
    private int DisGapple;
    private long time = -1L;
    public BoolValue unAttack = new BoolValue("UnTakeWhenGotHurt", true);
    public BoolValue unwrapC07 = new BoolValue("UnWrapC07", false);
    public BoolValue unwrapC08 = new BoolValue("UnWrapC08", false);
    public BoolValue unwrapC0A = new BoolValue("UnWrapC0A", false);
    public BoolValue unwrapC02 = new BoolValue("UnWrapC02", false);
    private final BoolValue NoMove = new BoolValue("NoMove", false);
    private final BoolValue NoPreMove = new BoolValue("NoPreMove", false);
    private final BoolValue NoPostMove = new BoolValue("NoPostMove", false);
    private final BoolValue NoUpdateMove = new BoolValue("NoUpdateMove", false);
    private final BoolValue noMoveInput = new BoolValue("NoMoveInput", false);
    private final BoolValue disableOnWorld = new BoolValue("DisableOnWorld", true);
    private final BoolValue noPostDis = new BoolValue("NoPostDis", false);
    private final BoolValue debug = new BoolValue("UnTakeC03", false);
    private final BoolValue debugC03 = new BoolValue("C03Debug", true);
    private boolean canCancleC03 = false;
    public TimeUtil timeUtil = new TimeUtil();
    public boolean attack = false;
    public ArrayList<Packet<?>> packets = new ArrayList();
    public ArrayList<C03PacketPlayer> c03 = new ArrayList();
    public int slotid = -1;

    @EventTarget
    public void onWorld(EventWorldLoad eventWorld) {
        this.setState(false);
    }

    public Gapple() {
        super("Gapple", Category.Player);
    }

    public void onDisable() {
        DisGapple = 0;
        Disabler.postValue.setValue(true);
        this.timeUtil.reset();
        this.slotid = -1;
        this.packets.clear();
        this.c03.clear();
        this.attack = false;
    }

    public void onEnable() {
        DisGapple = 0;
        Client.instance.moduleManager.getModule(NoSlow.class).setState(false);
        if ((Boolean)this.noPostDis.getValue()) {
            Disabler.postValue.setValue(false);
        }

        this.timeUtil.reset();
        this.slotid = -1;
        this.packets.clear();
        this.c03.clear();
        this.attack = false;
    }

    @EventTarget
    public void onSendPacket(EventPacketSend eventSendPacket) {
        Packet<?> p = eventSendPacket.getPacket();
        if (!(p instanceof C07PacketPlayerDigging)) {
            if (!(p instanceof C08PacketPlayerBlockPlacement) || !(Boolean)this.unwrapC08.getValue()) {
                if (!(p instanceof C0APacketAnimation) || !(Boolean)this.unwrapC0A.getValue()) {
                    if (!(p instanceof C02PacketUseEntity) || !(Boolean)this.unwrapC02.getValue()) {
                        this.packets.add(p);
                        if (p instanceof C03PacketPlayer) {
                            this.c03.add((C03PacketPlayer)p);
                            if ((Boolean)this.debug.getValue()) {
                                DebugUtil.log("CanCle C03");
                            }

                            if ((Boolean)this.debugC03.getValue()) {
                                DebugUtil.log(String.valueOf(this.c03.size()));
                                if(this.c03.size() == 5){
                                    ++ DisGapple;
                                    if (DisGapple == 3){
                                        DebugUtil.log("次数到达上线,自动关闭gapple以防止ban!");
                                        Client.instance.moduleManager.getModule(Gapple.class).setState(false);
                                    }
                                }
                            }
                        }

                        if ((Boolean)this.debug.getValue()) {
                            DebugUtil.log("Collected: " + p.toString());
                        }

                        eventSendPacket.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onReadPacket(EventPacketReceive eventReadPacket) {
        Packet<?> p = eventReadPacket.getPacket();
        if (p instanceof S12PacketEntityVelocity && ((S12PacketEntityVelocity)p).getEntityID() == mc.thePlayer.getEntityId()) {
            this.attack = true;
        } else {
            this.attack = false;
        }

    }

    @EventTarget
    public void onEventTick(EventTick eventTick) {
        if ((Boolean)this.noMoveInput.getValue()) {
            this.noMoveKey();
        }

        this.take();
        if (this.shouldGapple()) {
            if ((Boolean)this.debug.getValue()) {
                DebugUtil.log("AutoGapple: " + this.shouldGapple());
            }

            if (InventoryUtil.getGappleSlot() != -1 && (mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem) == null || !(mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem).getItem() instanceof ItemAppleGold))) {
                this.slotid = mc.thePlayer.inventory.currentItem;
                this.takeAll();
                mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
            } else if (mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem) != null && mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem).getItem() instanceof ItemAppleGold) {
                this.takeAll();
            }
        }

    }

    @EventTarget
    public void onEventUpdate(EventUpdate eventUpdate) {
        if ((Boolean)this.NoUpdateMove.getValue()) {
            EntityPlayerSP var10000 = mc.thePlayer;
            var10000.motionX *= 0.0;
            var10000 = mc.thePlayer;
            var10000.motionY *= 0.0;
            var10000 = mc.thePlayer;
            var10000.motionZ *= 0.0;
        }

    }

    @EventTarget
    public void onEventMove(EventMove eventMove) {
        if ((Boolean)this.NoMove.getValue()) {
            EntityPlayerSP var10000 = mc.thePlayer;
            var10000.motionX *= 0.0;
            var10000 = mc.thePlayer;
            var10000.motionY *= 0.0;
            var10000 = mc.thePlayer;
            var10000.motionZ *= 0.0;
        }

    }

    @EventTarget
    public void onEventPreMotion(EventMotion event) {
        EntityPlayerSP var10000;
        if (event.isPre() && (Boolean)this.NoPreMove.getValue()) {
            var10000 = mc.thePlayer;
            var10000.motionX *= 0.0;
            var10000 = mc.thePlayer;
            var10000.motionY *= 0.0;
            var10000 = mc.thePlayer;
            var10000.motionZ *= 0.0;
        }

        if (event.isPre() && (Boolean)this.NoPostMove.getValue()) {
            var10000 = mc.thePlayer;
            var10000.motionX *= 0.0;
            var10000 = mc.thePlayer;
            var10000.motionY *= 0.0;
            var10000 = mc.thePlayer;
            var10000.motionZ *= 0.0;
        }

    }

    public boolean shouldGapple() {
        if ((Boolean)this.debug.getValue()) {
            DebugUtil.log("C03: " + this.c03.size());
        }

        return !this.c03.isEmpty() && this.c03.size() >= 33;
    }

    public void noMoveKey() {
        mc.gameSettings.keyBindForward.pressed = false;
        mc.gameSettings.keyBindRight.pressed = false;
    }

    public void take() {
        if (!this.packets.isEmpty() && (!(Boolean)this.unAttack.getValue() || !this.attack)) {
            Packet<?> p = (Packet)this.packets.get(0);
            this.packets.remove(p);
            if (p instanceof C03PacketPlayer) {
                this.c03.remove((C03PacketPlayer)p);
            }

            PacketUtil.sendPacketNoEvent(p);
            if (this.attack) {
                this.attack = false;
            }

            if ((Boolean)this.debug.getValue()) {
                DebugUtil.log("Take: " + this.packets.get(0));
            }
        }

    }

    public void takeAll() {
        try {
            if (!this.packets.isEmpty()) {
                this.c03.clear();
            }

            if ((Boolean)this.debug.getValue()) {
                DebugUtil.log("Sent §2" + this.packets.size() + "§f Packets !");
            }
        } catch (NullPointerException var2) {
        } catch (Exception var3) {
            Exception var2 = var3;
        }

    }

}
