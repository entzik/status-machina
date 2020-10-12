/*****************************************************************************
 * Copyright (C) [2018 - PRESENT] LiquidShare
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of LiquidShare.
 *
 * The intellectual and technical concepts contained herein are proprietary
 *  to LiquidShare and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from LiquidShare.
 *****************************************************************************/
package io.statusmachina.core.api;

public interface StalledData extends StateMachineData {
    /**
     * @return the current state label of the machine
     */
    String getCurrentStateLabel();
}
