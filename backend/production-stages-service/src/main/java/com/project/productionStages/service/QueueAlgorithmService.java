package com.project.productionStages.service;

import org.springframework.stereotype.Service;

@Service
public class QueueAlgorithmService {

    public int getPreviousGroups(
            int queueNumber,
            int firstWaitingQueue,
            int lineCount
    ){

        int diff = queueNumber - firstWaitingQueue;

        int groups = diff / lineCount;

        return Math.max(groups,0);
    }

    public int getOrderInGroup(
            int queueNumber,
            int firstWaitingQueue,
            int lineCount
    ){

        return ((queueNumber - firstWaitingQueue) % lineCount) + 1;

    }

}
