package com.kingzcheung.xime.speech.models

import com.kingzcheung.xime.model.ModelFile

/** 固定来源与摘要；不从远程索引猜测后端或模型文件角色。 */
internal val AdditionalStreamingAsrProfiles = listOf(
    AsrModelProfile(
        id = "paraformer-streaming-zh-yue-en", name = "粤语与方言 · Paraformer三语",
        description = "普通话、粤语、英文与部分地方话；边说边出字，本地识别。", language = "zh-yue-en",
        engine = AsrEngine.STREAMING_PARAFORMER,
        size = "238.5MB", downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-streaming-paraformer-trilingual-zh-cantonese-en/resolve/86931b152d37c908528b44d30b431c24a19770da/encoder.int8.onnx",
        encoderFile = "encoder.int8.onnx", decoderFile = "decoder.int8.onnx",
        downloadFiles = listOf(
            ModelFile("encoder.int8.onnx", "https://huggingface.co/csukuangfj/sherpa-onnx-streaming-paraformer-trilingual-zh-cantonese-en/resolve/86931b152d37c908528b44d30b431c24a19770da/encoder.int8.onnx", "6047a644b41b236d9d8e89e3b94ef39d1b7037daab028131b722ca52e10b0357", 166362800L),
            ModelFile("decoder.int8.onnx", "https://huggingface.co/csukuangfj/sherpa-onnx-streaming-paraformer-trilingual-zh-cantonese-en/resolve/86931b152d37c908528b44d30b431c24a19770da/decoder.int8.onnx", "545427acf508452b7d89969be082c8128c681e3432ff43aef09f6159f4b61a7e", 72062549L),
            ModelFile("tokens.txt", "https://huggingface.co/csukuangfj/sherpa-onnx-streaming-paraformer-trilingual-zh-cantonese-en/resolve/86931b152d37c908528b44d30b431c24a19770da/tokens.txt", "45b31504211675dd52aa88f998a6f6161703a2834e86760c1cda645a22538085", 81289L),
        ),
    ),
    AsrModelProfile(
        id = "paraformer-streaming-zh-en", name = "中英与方言 · Paraformer",
        description = "普通话、中英混说与部分地方话；边说边出字，本地识别。", language = "zh-en",
        engine = AsrEngine.STREAMING_PARAFORMER,
        size = "237.2MB", downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-streaming-paraformer-bilingual-zh-en/resolve/8e40c43232a1c5c66c82111efc5820d3accca11b/encoder.int8.onnx",
        encoderFile = "encoder.int8.onnx", decoderFile = "decoder.int8.onnx",
        downloadFiles = listOf(
            ModelFile("encoder.int8.onnx", "https://huggingface.co/csukuangfj/sherpa-onnx-streaming-paraformer-bilingual-zh-en/resolve/8e40c43232a1c5c66c82111efc5820d3accca11b/encoder.int8.onnx", "81a70226a8934e6ed92aa1d4fc486b428b5398e2f2619ed4897b7294cab90e9a", 165462184L),
            ModelFile("decoder.int8.onnx", "https://huggingface.co/csukuangfj/sherpa-onnx-streaming-paraformer-bilingual-zh-en/resolve/8e40c43232a1c5c66c82111efc5820d3accca11b/decoder.int8.onnx", "f3cca9f77bb9d93c8fcbfb63ae617b6b1ee96818df3aa3b151c40658fe38594f", 71664561L),
            ModelFile("tokens.txt", "https://huggingface.co/csukuangfj/sherpa-onnx-streaming-paraformer-bilingual-zh-en/resolve/8e40c43232a1c5c66c82111efc5820d3accca11b/tokens.txt", "59aba8873a2ed1e122c25fee421e25f283b63290efbde85c1f01a853d83cb6e6", 75756L),
        ),
    ),
    AsrModelProfile(
        id = "dolphin-streaming-cn-base", name = "多地方话 · Dolphin轻量流式",
        description = "面向22种中文方言；边说边出字，CTC量化适配版，手机效果待验收。", language = "zh-dialects",
        engine = AsrEngine.DOLPHIN_STREAMING,
        size = "63.7MB", downloadUrl = "https://github.com/xiaoxi881217/open-source-projects/releases/download/keyboard-asr-models-20261112/dolphin-streaming-cn-base-int8-20261112.tar.bz2",
        archiveSha256 = "03f16787f15a1661bfd7fa2e6f6f789ef5611069e4342f67079bccead32abc55", modelFile = "model.int8.onnx",
        fileSizes = mapOf("model.int8.onnx" to 82036679L, "tokens.txt" to 204074L),
    ),
    AsrModelProfile(
        id = "zipformer-ctc-small-zh", name = "轻巧中文 · Zipformer CTC",
        description = "约25MiB中文流式模型；更省空间，不承诺方言或特殊发音更准。", language = "zh",
        engine = AsrEngine.STREAMING_CTC,
        size = "26.4MB", downloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-streaming-zipformer-small-ctc-zh-int8-2025-04-01/resolve/a5f60fe00dcfbaf68fcc1c6b5cf53061e144d6da/model.int8.onnx",
        modelFile = "model.int8.onnx",
        downloadFiles = listOf(
            ModelFile("model.int8.onnx", "https://huggingface.co/csukuangfj/sherpa-onnx-streaming-zipformer-small-ctc-zh-int8-2025-04-01/resolve/a5f60fe00dcfbaf68fcc1c6b5cf53061e144d6da/model.int8.onnx", "68c9c943840f7d9cf3e8a4970ba50f404feb5277f611fa82b7e72267786fa84a", 26342340L),
            ModelFile("tokens.txt", "https://huggingface.co/csukuangfj/sherpa-onnx-streaming-zipformer-small-ctc-zh-int8-2025-04-01/resolve/a5f60fe00dcfbaf68fcc1c6b5cf53061e144d6da/tokens.txt", "6fed8c6c248516f38e7faa19404b57413e8ce259f1cbc1fa4aebc86eac32fdfd", 13366L),
        ),
    ),
    AsrModelProfile(
        id = "zipformer-ctc-multi-zh", name = "多来源中文 · Zipformer CTC",
        description = "多来源中文语料流式模型；本地量化版，多来源不等于多方言。", language = "zh",
        engine = AsrEngine.STREAMING_CTC,
        size = "56.3MB", downloadUrl = "https://github.com/xiaoxi881217/open-source-projects/releases/download/keyboard-asr-models-20261112/zipformer-ctc-multi-zh-int8-20261112.tar.bz2",
        archiveSha256 = "0714799c1c0ab2f60b98bce61f78d95e37202f03fa1bf346c3f3c1f7db7b499f", modelFile = "model.int8.onnx",
        fileSizes = mapOf("model.int8.onnx" to 70872379L, "tokens.txt" to 18626L),
    ),
)
