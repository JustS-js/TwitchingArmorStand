package justs_js.twas.entity;

import com.google.common.collect.ImmutableSet;
import justs_js.twas.TWASModClient;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.function.Function;

public class TwitchingAnimator {
    private boolean animateMovement = true;
    private boolean animateJump = true;
    private boolean animateTwerk = false;
    private boolean animateHello = false;
    private boolean animateClap = false;
    private float size;

    private final Animation MOVEMENT;
    private final Animation JUMP;
    private final Animation TWERK;
    private final Animation HELLO;
    private final Animation CLAP;
    private final Set<Animation> animations;

    public void tick(Vec3 pos, Vec3 oldPos) {
        MOVEMENT.tick((float)(Math.abs(pos.x() - oldPos.x()) + Math.abs(pos.z() - oldPos.z())), size);
        JUMP.tick((float)(pos.y() - oldPos.y()), size);
        TWERK.tick(0f, size);
        HELLO.tick(0f, size);
        CLAP.tick(0f, size);
    }

    public TwitchingAnimator(float size) {
        this.size = size;
        this.animations = ImmutableSet.of(MOVEMENT, JUMP, TWERK, HELLO, CLAP);
    }

    public void setAnimateMovement(boolean animateMovement) {this.animateMovement = animateMovement;}
    public boolean getAnimateMovement() {return animateMovement;}
    public void setAnimateJump(boolean animateJump) {this.animateJump = animateJump;}
    public boolean getAnimateJump() {return animateJump;}
    public void setAnimateTwerk(boolean animateTwerk) {this.animateTwerk = animateTwerk;}
    public boolean getAnimateTwerk() {return animateTwerk;}
    public void setAnimateHello(boolean animateHello) {this.animateHello = animateHello;}
    public boolean getAnimateHello() {return animateHello;}
    public void setAnimateClap(boolean animateClap) {this.animateClap = animateClap;}
    public boolean getAnimateClap() {return animateClap;}

    public void setSize(float size) {
        this.size = size;
    }

    public CompoundTag getAnimatedPoseState() {
        CompoundTag poseTag = new CompoundTag();
        ListTag poseBodyTag = new ListTag();

        Triplet<Float> triplet = accumulate(Animation::getBody);
        poseBodyTag.add(FloatTag.valueOf(triplet.getFirst()));
        poseBodyTag.add(FloatTag.valueOf(triplet.getSecond()));
        poseBodyTag.add(FloatTag.valueOf(triplet.getThird()));
        poseTag.put("Body", poseBodyTag);

        ListTag poseLeftLegTag = new ListTag();
        triplet = accumulate(Animation::getLeftLeg);
        poseLeftLegTag.add(FloatTag.valueOf(triplet.getFirst()));
        poseLeftLegTag.add(FloatTag.valueOf(triplet.getSecond()));
        poseLeftLegTag.add(FloatTag.valueOf(triplet.getThird()));
        poseTag.put("LeftLeg", poseLeftLegTag);

        ListTag poseRightLegTag = new ListTag();
        triplet = accumulate(Animation::getRightLeg);
        poseRightLegTag.add(FloatTag.valueOf(triplet.getFirst()));
        poseRightLegTag.add(FloatTag.valueOf(triplet.getSecond()));
        poseRightLegTag.add(FloatTag.valueOf(triplet.getThird()));
        poseTag.put("RightLeg", poseRightLegTag);

        ListTag poseLeftArmTag = new ListTag();
        triplet = accumulate(Animation::getLeftArm);
        poseLeftArmTag.add(FloatTag.valueOf(triplet.getFirst()));
        poseLeftArmTag.add(FloatTag.valueOf(triplet.getSecond()));
        poseLeftArmTag.add(FloatTag.valueOf(triplet.getThird()));
        poseTag.put("LeftArm", poseLeftArmTag);

        ListTag poseRightArmTag = new ListTag();
        triplet = accumulate(Animation::getRightArm);
        poseRightArmTag.add(FloatTag.valueOf(triplet.getFirst()));
        poseRightArmTag.add(FloatTag.valueOf(triplet.getSecond()));
        poseRightArmTag.add(FloatTag.valueOf(triplet.getThird()));
        poseTag.put("RightArm", poseRightArmTag);

        return poseTag;
    }

    private Triplet<Float> accumulate(Function<Animation, Triplet<Float>> function) {
        float f = 0;
        float s = 0;
        float t = 0;
        for (Animation animation : animations) {
            Triplet<Float> result = function.apply(animation);
            f += result.getFirst();
            s += result.getSecond();
            t += result.getThird();
        }
        return Triplet.of(f, s ,t);
    }

    private abstract class Animation {
        protected final float animationSpeed;
        protected final float animationMultiplier;
        protected final float animationMaxAngle;
        protected float animationAngle = 0f;
        protected float animationIntensity = 0;
        protected float animationState = 0;

        Animation(float speed, float multiplier, float maxAngle) {
            this.animationSpeed = speed;
            this.animationMultiplier = multiplier;
            this.animationMaxAngle = maxAngle;
        }

        void tick(float delta, float size) {}

        Triplet<Float> getRightLeg() {return Triplet.of(0f, 0f, 0f);}
        Triplet<Float> getLeftLeg() {return Triplet.of(0f, 0f, 0f);}
        Triplet<Float> getRightArm() {return Triplet.of(0f, 0f, 0f);}
        Triplet<Float> getLeftArm() {return Triplet.of(0f, 0f, 0f);}
        Triplet<Float> getBody() {return Triplet.of(0f, 0f, 0f);}
    }

    private static class Triplet<T> {
        private final T first;
        private final T second;
        private final T third;

        Triplet(T f, T s, T t) {
            this.first = f;
            this.second = s;
            this.third = t;
        }

        static <E> Triplet<E> of(E f, E s, E t) {
            return new Triplet<>(f, s ,t);
        }

        public T getFirst() {
            return first;
        }

        public T getSecond() {
            return second;
        }

        public T getThird() {
            return third;
        }
    }

    {
        MOVEMENT = new Animation(0.3f, 3f, 60f){
            @Override
            void tick(float delta, float size) {
                if (!getAnimateMovement()) return;
                float appliedSpeed = this.animationSpeed * 1 / size;
                animationAngle = (float) ((animationAngle + appliedSpeed) % (2 * Math.PI));
                animationIntensity = Math.clamp(delta * animationMultiplier, 0f, 1f);
                animationState = (float) Math.sin(animationAngle) * animationMaxAngle * animationIntensity;
                if (Float.compare(0f, animationIntensity) == 1) {
                    animationAngle = 0;
                }
            }
            @Override
            Triplet<Float> getLeftLeg() {return Triplet.of(animationState * -1, 0f, 0f);}
            @Override
            Triplet<Float> getRightLeg() {return Triplet.of(animationState, 0f, 0f);}
            @Override
            Triplet<Float> getLeftArm() {return Triplet.of(animationState, 0f, 0f);}
            @Override
            Triplet<Float> getRightArm() {return Triplet.of(animationState * -1, 0f, 0f);}
        };

        JUMP = new Animation(2f, 3f, 30f){
            @Override
            void tick(float delta, float size) {
                if (!getAnimateJump()) return;
                delta *= delta * Math.signum(delta);
                float appliedSpeed = this.animationSpeed * 1 / size;
                animationIntensity = Math.clamp(Math.abs(delta) * animationMultiplier, 0f, 1f);
                animationAngle += appliedSpeed * Math.signum(delta);
                animationAngle = Math.clamp(animationAngle, 0, animationMaxAngle);
                animationState = animationAngle;
            }
            @Override
            Triplet<Float> getLeftLeg() {return Triplet.of(0f, 0f, animationState * -1);}
            @Override
            Triplet<Float> getRightLeg() {return Triplet.of(0f, 0f, animationState);}
            @Override
            Triplet<Float> getLeftArm() {return Triplet.of(0f, 0f, animationState * -1);}
            @Override
            Triplet<Float> getRightArm() {return Triplet.of(0f, 0f, animationState);}
        };

        TWERK = new Animation(0.1f, 8f, 15f) {
            private final int maxTicksLeft = 64;
            private int ticksLeft = maxTicksLeft;

            @Override
            void tick(float delta, float size) {
                if (!getAnimateTwerk()) return;
                if (ticksLeft-- <= 0) {
                    ticksLeft = maxTicksLeft;
                    animationState = 0;
                    animationAngle = 0;
                    setAnimateTwerk(false);
                    return;
                }

                float appliedSpeed = this.animationSpeed * 1 / size;
                animationAngle = (float) ((animationAngle + appliedSpeed) % (Math.PI * 2));
                animationState = (float) ((1-Math.cos(animationAngle * animationMultiplier)) * animationMaxAngle) / 2;
            }

            @Override
            Triplet<Float> getBody() {return Triplet.of(animationState, 0f, 0f);}
            Triplet<Float> getLeftLeg() {return Triplet.of(-animationState/3, 0f, -animationState/6);}
            Triplet<Float> getRightLeg() {return Triplet.of(-animationState/3, 0f, animationState/6);}
        };

        HELLO = new Animation(0.155f, 3f, 120f) {
            private final int maxTicksLeft = 40;
            private int ticksLeft = maxTicksLeft;

            @Override
            void tick(float delta, float size) {
                if (!getAnimateHello()) return;
                if (ticksLeft-- <= 0) {
                    ticksLeft = maxTicksLeft;
                    animationState = 0;
                    animationAngle = 0;
                    setAnimateHello(false);
                    return;
                }
                float appliedSpeed = this.animationSpeed * 1 / size;
                animationState = (float) ((animationState + appliedSpeed) % (Math.PI * 2));
                double checker = animationState * 2 / Math.PI;
                if (1 <= checker && checker <= 3) {
                    animationAngle = (float) (animationMaxAngle * (3 - Math.cos(animationState * 4f)) / 4);
                } else {
                    animationAngle = (float) (animationMaxAngle * (1 - Math.cos(animationState)) / 2);
                }
            }

            @Override
            Triplet<Float> getBody() {return Triplet.of(0f, 0f, animationAngle/animationMaxAngle*10);}
            @Override
            Triplet<Float> getRightArm() {return Triplet.of(0f, 0f, animationAngle);}
        };

        CLAP = new Animation(0.1f, 3f, 60f) {
            private final int maxTicksLeft = 60;
            private int ticksLeft = maxTicksLeft;

            @Override
            void tick(float delta, float size) {
                if (!getAnimateClap()) return;
                if (ticksLeft-- <= 0) {
                    ticksLeft = maxTicksLeft;
                    animationState = 0;
                    animationAngle = 0;
                    setAnimateClap(false);
                    return;
                }
                float appliedSpeed = this.animationSpeed * 1 / size;
                animationState = (float) ((animationState + appliedSpeed) % (Math.PI * 2));
                double checker = animationState * 7 / Math.PI;
                if (4 <= checker && checker <= 10) {
                    animationAngle = (float) (animationMaxAngle * (4 - Math.cos(animationState * 7f)) / 5);
                } else {
                    animationAngle = (float) (animationMaxAngle * (1 - Math.cos(animationState)) / 2);
                }
            }

            @Override
            Triplet<Float> getLeftArm() {return Triplet.of(-Math.clamp(animationAngle, 0f, animationMaxAngle*0.5f)/0.5f, 0f, animationAngle);}
            @Override
            Triplet<Float> getRightArm() {return Triplet.of(-Math.clamp(animationAngle, 0f, animationMaxAngle*0.5f)/0.5f, 0f, -animationAngle);}
        };
    }
}
