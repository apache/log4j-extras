/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.log4j.rule;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import org.apache.log4j.spi.LoggingEventFieldResolver;

/**
 * A helper class which converts infix expressions to postfix expressions
 * Currently grouping is supported, as well as all of the
 * Rules supported by <code>RuleFactory</code>
 *
 * NOTE: parsing is supported through the use of <code>StringTokenizer</code>,
 * which means all tokens in the expression must be separated by spaces.
 *
 * Supports grouping via parens, mult-word operands using single quotes,
 * and these operators:
 *
 * !        NOT operator
 * !=       NOT EQUALS operator
 * ==       EQUALS operator
 * ~=       CASE-INSENSITIVE equals operator
 * ||       OR operator
 * &&       AND operator
 * like     REGEXP operator
 * exists   NOT NULL operator
 * &lt      LESS THAN operator
 * &gt      GREATER THAN operator
 * &lt=     LESS THAN EQUALS operator
 * &gt=     GREATER THAN EQUALS operator
 *
 * @author Scott Deboy (sdeboy@apache.org)
 */

public class InFixToPostFix {
    /**
     * Precedence map.
     */
  private static final Map precedenceMap = new HashMap();
    /**
     * Operators.
     */
  private static final List operators = new Vector();


  static {
    operators.add("!");
    operators.add("!=");
    operators.add("==");
    operators.add("~=");
    operators.add("||");
    operators.add("&&");
    operators.add("like");
    operators.add("exists");
    operators.add("<");
    operators.add(">");
    operators.add("<=");
    operators.add(">=");

    //boolean precedence
    precedenceMap.put("<", new Integer(3));
    precedenceMap.put(">", new Integer(3));
    precedenceMap.put("<=", new Integer(3));
    precedenceMap.put(">=", new Integer(3));

    precedenceMap.put("!", new Integer(3));
    precedenceMap.put("!=", new Integer(3));
    precedenceMap.put("==", new Integer(3));
    precedenceMap.put("~=", new Integer(3));
    precedenceMap.put("like", new Integer(3));
    precedenceMap.put("exists", new Integer(3));

    precedenceMap.put("||", new Integer(2));
    precedenceMap.put("&&", new Integer(2));
  }
    /**
     * Convert in-fix expression to post-fix.
     * @param expression in-fix expression.
     * @return post-fix expression.
     */
  public String convert(final String expression) {
    return infixToPostFix(new CustomTokenizer(expression));
  }

    /**
     * Evaluates whether symbol is operand.
     * @param s symbol.
     * @return true if operand.
     */
  public static boolean isOperand(final String s) {
    String symbol = s.toLowerCase();
    return (!operators.contains(symbol));
  }

    /**
     * Determines whether one symbol precedes another.
     * @param s1 symbol 1
     * @param s2 symbol 2
     * @return true if symbol 1 precedes symbol 2
     */
  boolean precedes(final String s1, final String s2) {
    String symbol1 = s1.toLowerCase();
    String symbol2 = s2.toLowerCase();

    if (!precedenceMap.keySet().contains(symbol1)) {
      return false;
    }

    if (!precedenceMap.keySet().contains(symbol2)) {
      return false;
    }

    int index1 = ((Integer) precedenceMap.get(symbol1)).intValue();
    int index2 = ((Integer) precedenceMap.get(symbol2)).intValue();

    boolean precedesResult = (index1 < index2);

    return precedesResult;
  }

    /**
     * convert in-fix expression to post-fix.
     * @param tokenizer tokenizer.
     * @return post-fix expression.
     */
  String infixToPostFix(final CustomTokenizer tokenizer) {
    final String space = " ";
    StringBuffer postfix = new StringBuffer();

    Stack stack = new Stack();

    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();

      boolean inText = (token.startsWith("'") && (!token.endsWith("'")));
      if (inText) {
          while (inText && tokenizer.hasMoreTokens()) {
            token = token + " " + tokenizer.nextToken();
            inText = !(token.endsWith("'"));
        }
      }

      if ("(".equals(token)) {
        //recurse
        postfix.append(infixToPostFix(tokenizer));
        postfix.append(space);
      } else if (")".equals(token)) {
        //exit recursion level
        while (stack.size() > 0) {
          postfix.append(stack.pop().toString());
          postfix.append(space);
        }

        return postfix.toString();
      } else if (isOperand(token)) {
        postfix.append(token);
        postfix.append(space);
      } else {
        //operator..
        //peek the stack..if the top element has a lower precedence than token
        //(peeked + has lower precedence than token *),
        // push token onto the stack
        //otherwise, pop top element off stack and add to postfix string
        //in a loop until lower precedence or empty..then push token
        if (stack.size() > 0) {

          String peek = stack.peek().toString();

          if (precedes(peek, token)) {
            stack.push(token);
          } else {
            boolean bypass = false;

            do {
              if (
                (stack.size() > 0)
                  && !precedes(stack.peek().toString(), token)) {
                postfix.append(stack.pop().toString());
                postfix.append(space);
              } else {
                bypass = true;
              }
            } while (!bypass);

            stack.push(token);
          }
        } else {
          stack.push(token);
        }
      }
    }

    while (stack.size() > 0) {
      postfix.append(stack.pop().toString());
      postfix.append(space);
    }

    return postfix.toString();
  }

  public static class CustomTokenizer {
    private LinkedList linkedList = new LinkedList();
    public CustomTokenizer(String input) {
      int index = 0;
//      System.out.println("parsing: " + input);
      boolean inString = false;
      StringBuffer temp = new StringBuffer();
      while (index < input.length()) {
        String thisChar = input.substring(index, index + 1);
        if (inString) {
          if (thisChar.equals("'")) {
//            System.out.println("ending a delimited string");
            //end delimited string, add to linkedlist & continue
            inString = false;
            temp.append(thisChar);
            linkedList.add(temp.toString());
//            System.out.println("adding delimited string: " + temp.toString());
            temp.setLength(0);
          } else {
            temp.append(thisChar);
          }
        } else {
          if (thisChar.equals("'")) {
            //starting a delimited string
            inString = true;
            temp.append(thisChar);
//            System.out.println("starting a delimited string");
          } else {
            if (thisChar.equals(" ")) {
              //no need to add the space - just add the linked list
              if (!temp.toString().trim().equals("")) {
//                System.out.println("found space - adding string: " + temp.toString());
                linkedList.add(temp.toString());
              }
              temp.setLength(0);
            } else {
              //not a string delimited by single ticks or a space
              //collect values until keyword is matched or operator is encountered
              temp.append(thisChar);
              String tempString = temp.toString();
              //all fields except PROP. field can be added if present
              if (LoggingEventFieldResolver.getInstance().isField(tempString) &&
                  !tempString.toUpperCase().startsWith(LoggingEventFieldResolver.PROP_FIELD)) {
                linkedList.add(tempString);
//                System.out.println("adding non-prop field: " + tempString);
                temp.setLength(0);
              } else {
                //if building a property field, go until an operator is encountered
                if (tempString.toUpperCase().startsWith(LoggingEventFieldResolver.PROP_FIELD)) {
                  for (Iterator iter = operators.iterator();iter.hasNext();) {
                    //skip the NOT operator, since there is both a ! and !=, and ! will match (check not after we don't have a match)
                    String thisOperator = (String)iter.next();
                    if (thisOperator.equals("!")) {
                      continue;
                    }
                    if (tempString.endsWith(thisOperator)) {
                      String property = tempString.substring(0, tempString.indexOf(thisOperator));
                      if (!property.trim().equals("")) {
                        linkedList.add(property);
//                        System.out.println("adding property: " + property);
                      }
                      linkedList.add(thisOperator);
//                      System.out.println("adding operator: " + thisOperator);

                      temp.setLength(0);
                    }
                  }
                  //is ! the 2nd to last character?
                  if (tempString.length() > 2 && tempString.substring(tempString.length() - 2, tempString.length() - 1).equals("!")) {
                    if (!tempString.endsWith("!=")) {
                      String property = tempString.substring(0, tempString.indexOf("!"));
                      linkedList.add(property);
                      linkedList.add("!");
                      temp.setLength(0);
                      temp.append(tempString.substring(tempString.length() - 1));
                    }
                  }
                  if (tempString.endsWith("(")) {
                    String property = tempString.substring(0, tempString.indexOf("("));
//                    System.out.println("adding property: " + property + " and left paren");

                    if (!property.trim().equals("")) {
                      linkedList.add(property);
//                      System.out.println("adding property: " + property);
                    }
//                    System.out.println("adding (");
                    linkedList.add("(");
                    temp.setLength(0);
                  }
                  if (tempString.endsWith(")")) {
                    String property = tempString.substring(0, tempString.indexOf(")"));
                    if (!property.trim().equals("")) {
//                      System.out.println("adding property: " + property);
                      linkedList.add(property);
                    }
//                    System.out.println("adding )");
                    linkedList.add(")");
                    temp.setLength(0);
                  }
                } else {
                  for (Iterator iter = operators.iterator();iter.hasNext();) {
                    String thisOperator = (String)iter.next();
                    if (thisOperator.equals("!")) {
                      continue;
                    }
                    //handling operator equality below
                    if (!tempString.equals(thisOperator) && tempString.endsWith(thisOperator)) {
                      String firstPart = tempString.substring(0, tempString.indexOf(thisOperator));
                      if (!firstPart.trim().equals("")) {
                        linkedList.add(firstPart);
//                        System.out.println("adding first part: " + firstPart);
                      }
                      linkedList.add(thisOperator);
//                      System.out.println("adding operator: " + thisOperator);

                      temp.setLength(0);
                    }
                  }
                  //is ! the 2nd to last character?
                  if (tempString.length() > 2 && tempString.substring(tempString.length() - 2, tempString.length() - 1).equals("!")) {
                    if (!tempString.endsWith("!=")) {
                      String firstPart = tempString.substring(0, tempString.indexOf("!"));
                      linkedList.add(firstPart);
                      linkedList.add("!");
                      temp.setLength(0);
                      temp.append(tempString.substring(tempString.length() - 1));
                    }
                  }

                  for (Iterator iter = operators.iterator();iter.hasNext();) {
                    String thisOperator = (String)iter.next();
                    if (thisOperator.equals("!")) {
                      continue;
                    }
                    if (tempString.equals(thisOperator)) {
                      linkedList.add(thisOperator);
                      temp.setLength(0);
//                      System.out.println("adding operator: " + thisOperator);
                    }
                  }

                  if (tempString.endsWith("(")) {
                    String firstPart = tempString.substring(0, tempString.indexOf("("));
                    if (!firstPart.trim().equals("")) {
                      linkedList.add(firstPart);
//                      System.out.println("adding first part: " + firstPart);
                    }
                    linkedList.add("(");
//                    System.out.println("adding (");
                    temp.setLength(0);
                  }
                  if (tempString.endsWith(")")) {
                    String firstPart = tempString.substring(0, tempString.indexOf(")"));
                    if (!firstPart.trim().equals("")) {
//                      System.out.println("adding first part: " + firstPart);
                      linkedList.add(firstPart);
                    }
                    linkedList.add(")");
//                    System.out.println("adding  )");
                    temp.setLength(0);
                  }
                }
              }
            }
          }
        }
        index++;
      }
      if (temp.length() > 0) {
//        System.out.println("adding remaining text: " + temp.toString());
        linkedList.add(temp.toString());
        temp.setLength(0);
      }
//      System.out.println("linked list: " + linkedList);
    }

    public boolean hasMoreTokens() {
      return linkedList.size() > 0;
    }

    public String nextToken() {
      return linkedList.remove().toString();
    }
  }
}
