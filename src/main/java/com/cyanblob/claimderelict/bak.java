package com.cyanblob.claimderelict;

import fi.bugbyte.framework.screen.ScalableIconTextButton;
import fi.bugbyte.framework.screen.StageButton;
import fi.bugbyte.framework.screen.StageButton.clickHandler;
import fi.bugbyte.spacehaven.gui.GUI.SelectedElements;
import fi.bugbyte.spacehaven.stuff.FactionUtils.FactionSide;
import fi.bugbyte.spacehaven.world.Ship;
import fi.bugbyte.spacehaven.world.Ship.ShipSettings;
import fi.bugbyte.spacehaven.world.ShipHelper.ShipState;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class bak {

    private static boolean moddedClickHandler = false;
    private static ScalableIconTextButton purchaseButton;

    @Pointcut("call(void fi.bugbyte.spacehaven.gui.GUI.SelectedElements.addExploredDerelictShipStuff()) && within(fi.bugbyte..*)")
    public void addShipStuff() {
    }

    @After("addShipStuff()")
    public void updateGui(JoinPoint joinPoint) throws Throwable {
        SelectedElements _this = (SelectedElements) joinPoint.getThis();

        Field privateUriField = _this.getClass().getDeclaredField("selectedShip");
        privateUriField.setAccessible(true);
        Ship ship = (Ship) privateUriField.get(_this);

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

                if (moddedClickHandler == false) {
                    moddedClickHandler = true;
                    clickHandler originalClickHandler = claimShipButton.getClickHandler();
                    claimShipButton.setClickHandler(clicked(ship, originalClickHandler));
                }

                addSelectionButton.invoke(_this, (StageButton) claimShipButton);

            } catch (Exception e) {
                System.out.println(e);
            }
        }
    };

    clickHandler clicked(Ship ship, clickHandler onClick)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

        return new clickHandler() {
            public void clicked() {
                Field privateUriField;
                try {
                    privateUriField = ship.getClass().getDeclaredField("shipSettings");
                    privateUriField.setAccessible(true);
                    ShipSettings shipsettings;

                    try {
                        
                        // remove the "derelict" flag
                        shipsettings = (ShipSettings) privateUriField.get(ship);
                        shipsettings.state = ShipState.Normal;

                        // easy way to set `ship.claimable = true`
                        ship.abandon(FactionSide.NotSet, false, true);

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