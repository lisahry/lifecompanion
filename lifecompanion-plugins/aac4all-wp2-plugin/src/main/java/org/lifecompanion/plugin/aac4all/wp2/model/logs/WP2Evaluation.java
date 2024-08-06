package org.lifecompanion.plugin.aac4all.wp2.model.logs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WP2Evaluation {
    private Date date;
    private List<WP2KeyboardEvaluation> evaluations = new ArrayList<>();

    public WP2Evaluation(Date date) {
        this.date = date;
    }

    public List<WP2KeyboardEvaluation> getEvaluations() {
        return evaluations;
    }

    public Date getDate() {
        return date;
    }
}
