package com.tourya.api.models.responses;

public class ClearCartResponse {
    private int deletedItems;

    public ClearCartResponse(int deletedItems) {
        this.deletedItems = deletedItems;
    }

    public int getDeletedItems() {
        return deletedItems;
    }

    public void setDeletedItems(int deletedItems) {
        this.deletedItems = deletedItems;
    }
}
