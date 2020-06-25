package com.palyrobotics;

import com.palyrobotics.pipelines.OrangeCargoPipeline;
import com.palyrobotics.pipelines.RedCargoPipeline;

import java.util.List;

public class PipelineManager {
    private List<Pipeline> mPipelineSequence = List.of(new OrangeCargoPipeline(), new RedCargoPipeline());

    public void runPipelines() {
        Communication.dataToSend.clear();
        for (Pipeline pipeline : mPipelineSequence) {
            pipeline.start();
            pipeline.update();
            Communication.dataToSend.put(pipeline.getName(), pipeline.getData());
        }
    }
}
