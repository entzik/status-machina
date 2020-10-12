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
package io.statusmachina.spring.jpa.service;

import io.statusmachina.core.api.ErrorType;
import io.statusmachina.core.api.MachineSnapshot;
import io.statusmachina.core.api.StalledData;
import io.statusmachina.core.spi.StateMachineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "statusmachina.spring.statemachine.stalled.batch", havingValue = "true")
public class StateMachineStalledService {
    private final Logger LOGGER = LoggerFactory.getLogger(StateMachineStalledService.class);

    public static final String BEAN_STATE_MACHINE_STALLED_HANDLER = "BEAN_STATE_MACHINE_STALLED_HANDLER";

    @Autowired
    @Qualifier(BEAN_STATE_MACHINE_STALLED_HANDLER)
    Consumer<StalledData> stalledHandler;

    @Autowired
    private StateMachineService stateMachineService;

    public StateMachineStalledService() {
        String t = "";
    }

    private List<Pair<String, Long>> stateMachineIdAndEpochStalled = new ArrayList<>();

    @Value("#{new Integer('${statusmachina.spring.statemachine.stalled.seconds}')}")
    private int secondForStalledMachine;

    @Scheduled(fixedRateString = "#{1000 * new Integer('${statusmachina.spring.statemachine.stalled.batch.frequency.seconds}')}")
    public void findAndPublishStaledStateMachine() {
        LOGGER.debug("Starting findAndPublishStaledStateMachine");
        List<MachineSnapshot> stalledMachines = (List<MachineSnapshot>) stateMachineService.findStale(secondForStalledMachine);
        LOGGER.debug("found {} stalled machine", stalledMachines.size());

        Map<Pair<String, Long>, MachineSnapshot> machineStateById = stalledMachines.stream().filter(ms -> ((MachineSnapshot) ms).getErrorType() == ErrorType.NONE).collect(Collectors.toMap(ms -> Pair.of(ms.getId(), ms.getLastModifiedEpoch()), ms -> ms));
        for (MachineSnapshot stalledMachine : stalledMachines) {
            Pair<String, Long> pair = Pair.of(stalledMachine.getId(), stalledMachine.getLastModifiedEpoch());
            if (!stateMachineIdAndEpochStalled.contains(pair)) {
                stalledHandler.accept(new DefaultStalledData(stalledMachine));
                stateMachineIdAndEpochStalled.add(pair);
            }
        }
        stateMachineIdAndEpochStalled.retainAll(machineStateById.keySet());
    }

    private static class DefaultStalledData implements StalledData {
        MachineSnapshot machineSnapshot;

        public DefaultStalledData(MachineSnapshot machineSnapshot) {
            this.machineSnapshot = machineSnapshot;
        }

        @Override
        public String getStateMachineId() {
            return machineSnapshot.getId();
        }

        @Override
        public String getCurrentStateLabel() {
            return machineSnapshot.getCrtState();
        }

        @Override
        public String getStateMachineType() {
            return machineSnapshot.getType();
        }

        @Override
        public long getLastModifiedEpoch() {
            return machineSnapshot.getLastModifiedEpoch();
        }
    }
}
