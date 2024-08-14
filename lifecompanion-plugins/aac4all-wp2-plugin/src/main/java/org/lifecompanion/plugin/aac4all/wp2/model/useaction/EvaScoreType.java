package org.lifecompanion.plugin.aac4all.wp2.model.useaction;

public enum EvaScoreType {
    SCORE_1(1), SCORE_2(2), SCORE_3(3);

    private final int score;

    EvaScoreType(int score){
        this.score = score;
    }

    public int getScore() {
        return score;
    }
}
