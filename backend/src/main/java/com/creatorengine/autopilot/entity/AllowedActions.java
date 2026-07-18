package com.creatorengine.autopilot.entity;

/** Which actions the creator has permitted AI Autopilot to take during a conversation. */
public class AllowedActions {

    private boolean collectEmail = true;
    private boolean collectPhone = false;
    private boolean recommendProducts = true;
    private boolean updateContacts = true;
    private boolean addTags = true;
    private boolean notifyOwner = true;
    private boolean escalateToHuman = true;
    private boolean sendTemplates = false;
    private boolean triggerAutomations = false;

    public boolean getCollectEmail() { return collectEmail; }
    public void setCollectEmail(boolean v) { this.collectEmail = v; }

    public boolean getCollectPhone() { return collectPhone; }
    public void setCollectPhone(boolean v) { this.collectPhone = v; }

    public boolean getRecommendProducts() { return recommendProducts; }
    public void setRecommendProducts(boolean v) { this.recommendProducts = v; }

    public boolean getUpdateContacts() { return updateContacts; }
    public void setUpdateContacts(boolean v) { this.updateContacts = v; }

    public boolean getAddTags() { return addTags; }
    public void setAddTags(boolean v) { this.addTags = v; }

    public boolean getNotifyOwner() { return notifyOwner; }
    public void setNotifyOwner(boolean v) { this.notifyOwner = v; }

    public boolean getEscalateToHuman() { return escalateToHuman; }
    public void setEscalateToHuman(boolean v) { this.escalateToHuman = v; }

    public boolean getSendTemplates() { return sendTemplates; }
    public void setSendTemplates(boolean v) { this.sendTemplates = v; }

    public boolean getTriggerAutomations() { return triggerAutomations; }
    public void setTriggerAutomations(boolean v) { this.triggerAutomations = v; }
}
