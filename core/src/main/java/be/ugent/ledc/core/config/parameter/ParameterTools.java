package be.ugent.ledc.core.config.parameter;

import be.ugent.ledc.core.util.ListOperations;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ParameterTools
{
    public static <T> T extractParameter(Parameter<T> parameter, String[] args) throws ParameterException
    {
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals(parameter.getName())
                    || args[i].equals(parameter.getShortName()))
            {
                if (i + 1 < args.length)
                {
                    //Create
                    T t = parameter.getCreator().apply(args[i + 1]);

                    //Test
                    if (parameter.getVerifiers().keySet().stream().allMatch(test -> test.test(t)))
                    {
                        return t;
                    } else
                    {
                        throw new ParameterException(parameter
                                .getVerifiers()
                                .entrySet()
                                .stream()
                                .filter(e -> !e.getKey().test(t))
                                .findFirst()
                                .get()
                                .getValue());
                    }
                }
            }
        }

        if (parameter.isRequired())
        {
            throw new ParameterException("Required parameter " + parameter.getName() + " is missing.");
        }
        else if(parameter instanceof OptionalParameter)
        {
            return ((OptionalParameter<T>)parameter).getDefaultValue();
        }

        return null;
    }
    
    public static <T> List<T> extractMultiParameter(Parameter<T> parameter, String[] args) throws ParameterException
    {
        if(!parameter.isRepeating())
            throw new ParameterException("Repetition of parameter " + parameter.getName() + " is not allowed.");
        
        List<T> values = new ArrayList<>();
        
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals(parameter.getName())
            ||  args[i].equals(parameter.getShortName()))
            {
                if (i + 1 < args.length)
                {
                    //Create
                    T t = parameter.getCreator().apply(args[i + 1]);

                    //Test
                    if (parameter.getVerifiers().keySet().stream().allMatch(test -> test.test(t)))
                    {
                        values.add(t);
                    }
                    else
                    {
                        throw new ParameterException(parameter
                            .getVerifiers()
                            .entrySet()
                            .stream()
                            .filter(e -> !e.getKey().test(t))
                            .findFirst()
                            .get()
                            .getValue());
                    }
                }
            }
        }
        
        if(!values.isEmpty())
            return values;

        if (parameter.isRequired())
        {
            throw new ParameterException("Required parameter " + parameter.getName() + " is missing.");
        }
        else if(parameter instanceof OptionalParameter)
        {
            return ListOperations.list(((OptionalParameter<T>)parameter).getDefaultValue());
        }
        else
        {
            return values;
        }
    }

    public static boolean extractFlag(Flag flag, String[] args)
    {
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals(flag.getName())
                    || args[i].equals(flag.getShortName()))
            {
                return true;
            }
        }

        return false;
    }

    public static String unknownArgumentCheck(String[] args, Parameter<?>[] parameters, Flag[] flags)
    {
        for (int i = 0; i < args.length; i++)
        {
            String a = args[i];

            String pa = i == 0 ? "" : args[i - 1];

            if (a.isEmpty()
                    || (Stream.of(parameters).noneMatch(p -> p.getName().equals(a) || p.getShortName().equals(a))
                    && Stream.of(parameters).noneMatch(p -> p.getName().equals(pa) || p.getShortName().equals(pa))
                    && Stream.of(flags).noneMatch(f -> f.getName().equals(a) || f.getShortName().equals(a))))
            {
                return args[i];
            }
        }
        
        return null;
    }
    
    public static String unknownArgumentCheck(String[] args, List<Parameter<?>> parameters, List<Flag> flags)
    {
        return unknownArgumentCheck(
            args,
            parameters.toArray(new Parameter[parameters.size()]),
            flags.toArray(new Flag[flags.size()])
        );
    }
    
    public static int locateParameter(Parameter<?> parameter, String[] args) throws ParameterException
    {
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals(parameter.getName())
                    || args[i].equals(parameter.getShortName()))
            {
                if (i + 1 < args.length)
                {
                    return i;

                }
            }
        }

        return -1;
    }
    
    public static int locateFlag(Flag flag, String[] args)
    {
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals(flag.getName())
                    || args[i].equals(flag.getShortName()))
            {
                return i;
            }
        }

        return -1;
    }
}
