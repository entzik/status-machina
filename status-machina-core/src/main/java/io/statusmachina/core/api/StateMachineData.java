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

public interface StateMachineData {
    /**
     * @return the id of the machine
     */
    String getStateMachineId();

    /**
     * @return the machine type of the machine
     */
    String getStateMachineType();

    /**
     * @return the last modified epoch in milli seconds of the machine
     */
    long getLastModifiedEpoch();
}
