package com.cyanblob.claimderelict;

import fi.bugbyte.framework.Game;
import fi.bugbyte.framework.files.CompiledClassLoader;
import fi.bugbyte.framework.screen.ScalableIconTextButton;
import fi.bugbyte.framework.screen.StageButton;
import fi.bugbyte.framework.screen.StageButton.clickHandler;
import fi.bugbyte.gen.compiled.TextButtons2;
import fi.bugbyte.gen.compiled.TextIconButton1;
import fi.bugbyte.spacehaven.gui.Indicators;
import fi.bugbyte.spacehaven.gui.GUI.SelectedElements;
import fi.bugbyte.spacehaven.gui.GameLog;
import fi.bugbyte.spacehaven.stuff.FactionUtils.FactionSide;
import fi.bugbyte.spacehaven.world.Ship;
import fi.bugbyte.spacehaven.world.World;
import fi.bugbyte.spacehaven.world.Ship.ShipSettings;
import fi.bugbyte.spacehaven.world.ShipHelper.ShipState;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class claimDerelictAspect {

    private static ScalableIconTextButton purchaseButton;
    private World world = null;

    @Pointcut("call(void fi.bugbyte.spacehaven.gui.GUI.SelectedElements.addExploredDerelictShipStuff()) && within(fi.bugbyte..*)")
    public void addShipStuff() {
    }

    @After("addShipStuff()")
    public void updateGui(JoinPoint joinPoint) throws Throwable {
        SelectedElements _this = (SelectedElements) joinPoint.getThis();

        Field privateUriField = _this.getClass().getDeclaredField("selectedShip");
        privateUriField.setAccessible(true);
        Ship ship = (Ship) privateUriField.get(_this);

        if (world == null) {
            world = ship.getWorld();
        }

        Method addSelectionButton = _this.getClass().getDeclaredMethod("addSelectionButton", StageButton.class);
        addSelectionButton.setAccessible(true);
        Method createClaimButton = _this.getClass().getDeclaredMethod("createClaimButton");
        createClaimButton.setAccessible(true);

        if (ship.isDerelict() && !ship.isUnexplored() && !ship.isPlayerShip()) {
            try {
                createClaimButton.invoke(_this);

                privateUriField = _this.getClass().getDeclaredField("claimShipButton");
                privateUriField.setAccessible(true);
                ScalableIconTextButton claimShipButton = (ScalableIconTextButton) privateUriField.get(_this);

                int price = 1000;
                purchaseButton = (ScalableIconTextButton) getPurchaseButton(price);

                clickHandler originalClickHandler = claimShipButton.getClickHandler();
                purchaseButton.setClickHandler(claimDerelictClickHandler(ship, originalClickHandler, price, world));

                addSelectionButton.invoke(_this, (StageButton) purchaseButton);

            } catch (Exception e) {
                System.out.println(e);
            }
        }
    };

    TextIconButton1 getPurchaseButton(int price) {
        boolean bool = CompiledClassLoader.canCallOnGet;
        CompiledClassLoader.canCallOnGet = false;
        TextIconButton1 purchaseButton = TextButtons2.getIconBase2();
        CompiledClassLoader.canCallOnGet = bool;

        purchaseButton.setText("Purchase derelict: " + price + " credits");
        purchaseButton.toolTipText = "Allows purchasing a derelict ship";
        purchaseButton.icon = Game.library.getAnimation("claimShipButtonIcon", false);
        if (CompiledClassLoader.canCallOnGet)
            purchaseButton.onGet();

        return purchaseButton;
    }

    clickHandler claimDerelictClickHandler(Ship ship, clickHandler onClick, int price, World world)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

        return new clickHandler() {
            public void clicked() {
                Field privateUriField;
                Indicators.Bank playerBank = null;

                try {
                    privateUriField = world.getClass().getDeclaredField("playerBank");
                    privateUriField.setAccessible(true);
                    playerBank = (Indicators.Bank) privateUriField.get(world);

                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                try {

                    if (playerBank == null || playerBank.getCreditsAvailable() < price) {
                        GameLog.addLog("Can not afford to purchase derelict", GameLog.LogType.Failure, ship);
                        return;
                    }

                    privateUriField = ship.getClass().getDeclaredField("shipSettings");
                    privateUriField.setAccessible(true);
                    ShipSettings shipsettings;

                    try {
                        // remove the "derelict" flag
                        shipsettings = (ShipSettings) privateUriField.get(ship);
                        shipsettings.state = ShipState.Normal;

                        // easy way to set `ship.claimable = true`
                        ship.abandon(FactionSide.NotSet, false, true);

                        playerBank.addCredits(-price);

                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                onClick.clicked();
            }
        };

    }
}