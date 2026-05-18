package be.ugent.ledc.core.binding.csv;

import be.ugent.ledc.core.dataset.DataObject;

public class CSVLineParser
{
    public static DataObject parseLine(String line, CSVProperties csvProps)
    {
        //Initialize
        DataObject o = new DataObject();
        
        Character q = csvProps.getQuoteCharacter();
        String c = csvProps.getColumnSeparator();
        
        if(line.trim().isEmpty())
        {
            return o;
        }
        
        if(!line.contains(c))
        {
            if(q != null)
            {
                line = line.replace("" + q, "");
            }
            
            addAttribute(o, "0", line, csvProps);
        }
        else if(q == null)
        {
            //Split
            String[] columns = line.split(c, -1);
            //String[] columns = line.split(c);
            
            for(int i=0; i<columns.length; i++)
            {
                addAttribute(o, ""+i, columns[i], csvProps);
            }

        }
        else
        {
            Integer attributeIndex = 0;

            boolean quoteFlag = false;
            
            StringBuilder buffer = new StringBuilder();
            
            for(int i=0; i<line.length(); i++)
            {
                //Get next character
                Character ch = line.charAt(i);
                
                if(quoteFlag)
                {
                    //Reach 'end of quote'
                    if(ch.equals(q))
                    {
                        if(i+1<line.length() && line.charAt(i+1) == q)
                        {
                            buffer.append(ch);
                            i++;
                        }
                        else
                        {
                            //Flip the quote flag
                            quoteFlag = false;
                        }
                    }
                    else
                    {
                        buffer.append(ch);
                    }
                }
                else
                {
                    if(ch.equals(q) && buffer.length() == 0)
                    {
                        quoteFlag = true;
                    }
                    else if(ch.toString().equals(c))
                    {
                        //Add attribute with buffer contents
                        addAttribute(
                            o,
                            attributeIndex.toString(),
                            buffer.toString(),
                            csvProps);

                        //Increase counter
                        attributeIndex++;

                        //Clear the buffer
                        buffer.delete(0, buffer.length());
                    }
                    else
                    {
                        buffer.append(ch);
                    }
                }
            }
            
            //Flush buffer: add attribute with buffer contents
            addAttribute(
                o,
                attributeIndex.toString(),
                buffer.toString(),
                csvProps
            );
        }

        //Return
        return o;
    }
    
    private static void addAttribute(DataObject o, String attributeName, String attributeValue, CSVProperties csvProps)
    {
        String trimmedValue = attributeValue.trim();
        o.setString(
            attributeName,
            csvProps
            .getNullSymbols()
            .contains(trimmedValue)
                ? null
                : trimmedValue
        );
    }
}
