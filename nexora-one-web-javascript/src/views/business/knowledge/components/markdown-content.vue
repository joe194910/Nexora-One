<template>
  <div class="knowledge-markdown" v-html="renderedContent"></div>
</template>

<script setup>
import { computed } from 'vue';
import DOMPurify from 'dompurify';
import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js/lib/core';
import bash from 'highlight.js/lib/languages/bash';
import css from 'highlight.js/lib/languages/css';
import java from 'highlight.js/lib/languages/java';
import javascript from 'highlight.js/lib/languages/javascript';
import json from 'highlight.js/lib/languages/json';
import python from 'highlight.js/lib/languages/python';
import sql from 'highlight.js/lib/languages/sql';
import typescript from 'highlight.js/lib/languages/typescript';
import xml from 'highlight.js/lib/languages/xml';

hljs.registerLanguage('bash', bash);
hljs.registerLanguage('css', css);
hljs.registerLanguage('java', java);
hljs.registerLanguage('javascript', javascript);
hljs.registerLanguage('json', json);
hljs.registerLanguage('python', python);
hljs.registerLanguage('sql', sql);
hljs.registerLanguage('typescript', typescript);
hljs.registerLanguage('xml', xml);
hljs.registerAliases(['html', 'vue'], { languageName: 'xml' });
hljs.registerAliases(['js'], { languageName: 'javascript' });
hljs.registerAliases(['ts'], { languageName: 'typescript' });
hljs.registerAliases(['sh', 'shell'], { languageName: 'bash' });

const props = defineProps({
  content: {
    type: String,
    default: '',
  },
});

const markdown = new MarkdownIt({
  breaks: true,
  html: false,
  linkify: true,
  highlight(code, language) {
    if (language && hljs.getLanguage(language)) {
      const highlighted = hljs.highlight(code, {
        language,
        ignoreIllegals: true,
      }).value;
      return `<pre class="hljs"><code>${highlighted}</code></pre>`;
    }
    return `<pre class="hljs"><code>${markdown.utils.escapeHtml(code)}</code></pre>`;
  },
});

const defaultLinkOpen =
  markdown.renderer.rules.link_open ||
  ((tokens, index, options, env, renderer) => renderer.renderToken(tokens, index, options));

markdown.renderer.rules.link_open = (tokens, index, options, env, renderer) => {
  tokens[index].attrSet('target', '_blank');
  tokens[index].attrSet('rel', 'noopener noreferrer');
  return defaultLinkOpen(tokens, index, options, env, renderer);
};

const renderedContent = computed(() =>
  DOMPurify.sanitize(markdown.render(props.content || ''), {
    ADD_ATTR: ['target', 'rel'],
    ALLOW_DATA_ATTR: false,
  })
);
</script>

<style lang="less">
.knowledge-markdown {
  min-width: 0;
  color: #273444;
  font-size: 14px;
  line-height: 1.72;
  overflow-wrap: anywhere;

  > :first-child {
    margin-top: 0;
  }

  > :last-child {
    margin-bottom: 0;
  }

  p {
    margin: 0 0 10px;
  }

  h1,
  h2,
  h3,
  h4,
  h5,
  h6 {
    margin: 20px 0 10px;
    color: #182230;
    font-weight: 650;
    line-height: 1.35;
  }

  h1 {
    padding-bottom: 8px;
    border-bottom: 1px solid #e1e7ef;
    font-size: 22px;
  }

  h2 {
    font-size: 19px;
  }

  h3 {
    font-size: 16px;
  }

  h4,
  h5,
  h6 {
    font-size: 14px;
  }

  ul,
  ol {
    margin: 8px 0 12px;
    padding-left: 24px;
  }

  li {
    margin: 4px 0;
  }

  li > p {
    margin-bottom: 4px;
  }

  blockquote {
    margin: 12px 0;
    padding: 9px 12px;
    border-left: 3px solid #6f91b5;
    background: #f6f8fb;
    color: #4d5f72;
  }

  blockquote > :last-child {
    margin-bottom: 0;
  }

  a {
    color: #1768c5;
    text-decoration: none;
  }

  a:hover {
    text-decoration: underline;
  }

  code {
    padding: 2px 5px;
    border: 1px solid #e1e6ec;
    border-radius: 4px;
    background: #f2f4f7;
    color: #b42318;
    font-family: Consolas, Monaco, 'Courier New', monospace;
    font-size: 0.9em;
  }

  pre {
    max-width: 100%;
    margin: 12px 0;
    overflow: auto;
    border: 1px solid #dfe5ec;
    border-radius: 6px;
    background: #f6f8fa;
  }

  pre code {
    display: block;
    padding: 14px 16px;
    border: 0;
    background: transparent;
    color: inherit;
    line-height: 1.55;
    white-space: pre;
  }

  table {
    display: block;
    width: max-content;
    min-width: 100%;
    max-width: 100%;
    margin: 12px 0;
    overflow-x: auto;
    border-collapse: collapse;
  }

  th,
  td {
    min-width: 100px;
    padding: 8px 10px;
    border: 1px solid #dfe5ec;
    text-align: left;
    vertical-align: top;
  }

  th {
    background: #f4f6f8;
    color: #26384a;
    font-weight: 600;
  }

  tr:nth-child(even) td {
    background: #fafbfc;
  }

  img {
    display: block;
    max-width: 100%;
    height: auto;
    margin: 12px 0;
    border: 1px solid #e2e7ed;
    border-radius: 6px;
  }

  hr {
    margin: 20px 0;
    border: 0;
    border-top: 1px solid #dfe5ec;
  }

  input[type='checkbox'] {
    margin-right: 6px;
  }

  .hljs-comment,
  .hljs-quote {
    color: #6a737d;
  }

  .hljs-keyword,
  .hljs-selector-tag,
  .hljs-subst {
    color: #d73a49;
  }

  .hljs-number,
  .hljs-literal,
  .hljs-variable,
  .hljs-template-variable,
  .hljs-tag .hljs-attr {
    color: #005cc5;
  }

  .hljs-string,
  .hljs-doctag {
    color: #032f62;
  }

  .hljs-title,
  .hljs-section,
  .hljs-selector-id {
    color: #6f42c1;
  }

  .hljs-type,
  .hljs-class .hljs-title,
  .hljs-attribute,
  .hljs-name,
  .hljs-tag {
    color: #22863a;
  }

  .hljs-built_in,
  .hljs-builtin-name {
    color: #e36209;
  }

  .hljs-meta,
  .hljs-symbol,
  .hljs-bullet {
    color: #735c0f;
  }
}
</style>
