package com.example.finalyearproject.Utility;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@SessionScope
public class ProductSessionManager {
    private List<Integer> shuffledProductIds = null;

    public void setShuffledProductIds(List<Integer> productIds) {
        List<Integer> shuffledIds = new ArrayList<>(productIds);
        Collections.shuffle(shuffledIds);
        this.shuffledProductIds = shuffledIds;
    }

    public List<Integer> getShuffledProductIds() {
        return shuffledProductIds;
    }

    public boolean hasShuffledIds() {
        return shuffledProductIds != null && !shuffledProductIds.isEmpty();
    }

    public List<Integer> getPageOfIds(int page, int size) {
        if (!hasShuffledIds()) {
            return Collections.emptyList();
        }

        int startIndex = page * size;
        if (startIndex >= shuffledProductIds.size()) {
            return Collections.emptyList();
        }

        int endIndex = Math.min(startIndex + size, shuffledProductIds.size());
        return shuffledProductIds.subList(startIndex, endIndex);
    }
}