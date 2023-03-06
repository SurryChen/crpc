package com.somecode.core.loadBalance.random;

import com.somecode.common.entity.NetworkNode;
import com.somecode.common.spi.core.LoadBalance;

import java.util.List;
import java.util.Random;

public class RandomLoadBalance implements LoadBalance {

    /**
     * 随机负载均衡
     * @param networkNodeList
     * @return
     */
    @Override
    public NetworkNode requireBetterNodeFromList(List<NetworkNode> networkNodeList) {
        int total = networkNodeList.size();
        Random random = new Random();
        int index = random.nextInt(total);
        return networkNodeList.get(index);
    }

}
