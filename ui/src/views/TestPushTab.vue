<template>
  <div class="test-push-tab">
    <p class="hint">
      请先在「测试配置」Tab 保存群号/QQ 号，再点击下方按钮。将从已发布内容中随机或按最新选取进行推送。
    </p>
    <VSpace>
      <VButton type="primary" :loading="loadingPost" @click="sendTest('post')">
        随机发送一篇文章
      </VButton>
      <VButton type="secondary" :loading="loadingMomentRandom" @click="sendTest('moment', 'random')">
        随机发送一个瞬间
      </VButton>
      <VButton type="secondary" :loading="loadingMomentLatest" @click="sendTest('moment', 'latest')">
        发送最新瞬间
      </VButton>
    </VSpace>
    <p v-if="message" class="result">{{ message }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { axiosInstance } from "@halo-dev/api-client";
import { Toast, VButton, VSpace } from "@halo-dev/components";

const loadingPost = ref(false);
const loadingMomentRandom = ref(false);
const loadingMomentLatest = ref(false);
const message = ref("");

async function sendTest(templateType: "post" | "moment", mode?: "random" | "latest") {
  const loading =
    templateType === "post"
      ? loadingPost
      : mode === "latest"
        ? loadingMomentLatest
        : loadingMomentRandom;
  loading.value = true;
  message.value = "";
  try {
    const { data } = await axiosInstance.post(
      "/apis/console.api.xlingran-shan.halo.run/v1alpha1/test-push",
      { templateType, mode: mode ?? "random" }
    );
    if (templateType === "moment") {
      const preview = data.contentPreview ? `「${data.contentPreview}」` : "";
      const upvote = data.upvote != null ? ` 👍${data.upvote}` : "";
      message.value = `已推送瞬间 ${data.name ?? ""}${preview}${upvote}，请在 QQ 查看渲染效果。`;
    } else {
      const title = data.title ? `《${data.title}》` : "";
      message.value = `已推送文章 ${data.name ?? ""}${title}，请在 QQ 查看渲染效果。`;
    }
    Toast.success("测试推送已发送");
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || "推送失败";
    message.value = msg;
    Toast.error(msg);
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.test-push-tab {
  padding: 16px;
}
.hint {
  margin-bottom: 16px;
  color: var(--color-text-secondary, #666);
}
.result {
  margin-top: 16px;
}
</style>
