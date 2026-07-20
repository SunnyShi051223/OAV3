<template>
  <div class="layout">
    <aside>
      <h1>OA</h1>
      <nav>
        <template v-for="item in menus" :key="item.menuId">
          <RouterLink v-if="!item.children || item.children.length === 0" :to="item.menuPath">
            {{ item.menuName }}
          </RouterLink>
          <div v-else class="nav-group">
            <RouterLink :to="resolveMenuTarget(item)">{{ item.menuName }}</RouterLink>
            <div class="nav-children">
              <RouterLink v-for="child in item.children" :key="child.menuId" :to="child.menuPath">
                {{ child.menuName }}
              </RouterLink>
            </div>
          </div>
        </template>
      </nav>
    </aside>
    <main>
      <slot />
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { getCurrentMenuTree } from '../api/menus';

const menus = ref([]);

onMounted(async () => {
  const result = await getCurrentMenuTree();
  menus.value = result.data || [];
});

function resolveMenuTarget(item) {
  return item.children?.[0]?.menuPath || item.menuPath || '/dashboard';
}
</script>
