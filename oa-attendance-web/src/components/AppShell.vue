<template>
  <div class="layout">
    <aside>
      <h1>OA</h1>
      <nav>
        <RouterLink to="/dashboard">工作台</RouterLink>
        <RouterLink to="/documents">制度检索</RouterLink>
      </nav>
    </aside>
    <main>
      <slot />
    </main>
  </div>
</template>
