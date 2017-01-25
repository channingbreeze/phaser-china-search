var cheerio = require('cheerio');
var fs = require('fs');

// 加载文件
var contentText = fs.readFileSync(process.argv[2], 'utf-8');
$ = cheerio.load(contentText);

// 抓取constructor名字
var extractConstructorName = function(string) {
  var beginIndex = 0, endIndex = 0;
  if(string.indexOf("new") != -1) {
    beginIndex = string.indexOf("new") + 4;
  }
  endIndex = string.indexOf("(");
  if(beginIndex > endIndex) {
    beginIndex = 0;
  }
  return string.substring(beginIndex, endIndex).replace(/(^\s*)|(\s*$)/g, "");
}

// 抓取member名字
var extractMemberName = function(string) {
  var beginIndex = 0, endIndex = 0;
  if(string.indexOf(">") != -1) {
    beginIndex = string.indexOf(">") + 1;
  }
  endIndex = string.indexOf(":");
  if(beginIndex > endIndex) {
    beginIndex = 0;
  }
  return string.substring(beginIndex, endIndex).replace(/(^\s*)|(\s*$)/g, "");
}

// 抓取method名字
var extractMethodName = function(string) {
  var beginIndex = 0, endIndex = 0;
  if(string.indexOf(">") != -1) {
    beginIndex = string.indexOf(">") + 1;
  }
  endIndex = string.indexOf("(");
  if(beginIndex > endIndex) {
    beginIndex = 0;
  }
  return string.substring(beginIndex, endIndex).replace(/(^\s*)|(\s*$)/g, "");
}

// 结果数组
var res = [];

// 预处理h3，为了计算anchor
var h3Anchor = {};
var h3s = $("h3");
for(var i=0; i<h3s.length; i++) {
  h3Anchor[$(h3s[i]).text()] = true;
}

// 计算anchor的变量
var lastItem = "";
var memberNum = 0;
var methodNum = 0;
// 从h4.name下手
var names = $("h4.name");
for(var i=0; i<names.length; i++) {
  var item = {};
  // 获取name
  item.name = $(names[i]).text();
  // 获取description
  var des = $(names[i]).parent().next().find('div.description');
  if(des) {
    var texts = $(des).text();
    item.description = texts.replace(/(^\s*)|(\s*$)/g, "");
  } else {
    item.description = "";
  }
  // 获取type
  if(i == 0) {
    item.type = "constructor";
  } else if(item.name.indexOf("(") != -1 && item.name.indexOf(")") != -1) {
    item.type = "method";
  } else {
    item.type = "member";
  }
  // 赋值title，计算anchor
  if(item.type == "constructor") {
    item.anchor = "toc0";
    item.title = extractConstructorName(item.name);
  } else if(item.type == "member") {
    if(memberNum == 0) {
      if(h3Anchor["Extends"]) {
        item.anchor = "toc3";
      } else {
        item.anchor = "toc2";
      }
    } else {
      item.anchor = lastItem;
    }
    lastItem = extractMemberName(item.name);
    item.title = lastItem;
    memberNum++;
  } else if(item.type == "method") {
    if(methodNum == 0) {
      if(h3Anchor["Extends"]) {
        item.anchor = "toc" + (4 + memberNum);
      } else {
        item.anchor = "toc" + (3 + memberNum);
      }
    } else {
      item.anchor = lastItem;
    }
    lastItem = extractMethodName(item.name);
    item.title = lastItem;
    methodNum++;
  }
  // 放入结果数组
  res.push(item);
}

console.log(res);
