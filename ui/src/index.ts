import { markRaw } from "vue";

import { definePlugin } from "@halo-dev/ui-shared";

import TestPushTab from "./views/TestPushTab.vue";



export default definePlugin({

  components: {},

  routes: [],

  ucRoutes: [],

  extensionPoints: {

    "plugin:self:tabs:create": () => [

      {

        id: "actions",

        label: "发送测试",

        component: markRaw(TestPushTab),

      },

    ],

  },

});


