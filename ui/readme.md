
一些常用的ui 组件放在这个库

## 同意条款功能组件

- 核心需求：
  - 可以根据上架的区域不同（是否海外）决定是否显示勾选框，显示勾选框时需要提供可以获取选中状态的方法。
  - 条款名的颜色需要与其他文案不同，可以根据需求决定是否显示下划线，点击条款名时可以查看条款的具体内容。
- 设计思路：
  通过匹配字符串的方式来定位高亮和点击的位置

